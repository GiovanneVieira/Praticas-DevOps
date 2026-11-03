#!/usr/bin/env bash
# ============================================================================
# Wrapper das campanhas de carga do Desafio 1 — fino por design.
# A topologia vive no docker-compose.yaml (perfil `loadtest`); este script
# apenas: seleciona a fase, isola o projeto (-p pratica5-loadtest), resolve o
# contexto de build da API (worktree p/ baseline), aciona o JFR de pinning
# (fase VT) e coleta as evidências.
#
# Uso: ./run-loadtest.sh <baseline|pos-modulith|virtual-threads>
# ============================================================================
set -euo pipefail

FASE=${1:?uso: run-loadtest.sh <baseline|pos-modulith|virtual-threads>}
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PROJETO=pratica5-loadtest
EVIDENCIAS="$REPO_ROOT/docs/arquitetura/evidencias/desafio1/$FASE"
WORKTREE=""

case "$FASE" in
  baseline)
    # código pré-modularização (RF01+RF02 verdes em camadas) num worktree
    WORKTREE=/tmp/opencode/baseline-camadas
    git -C "$REPO_ROOT" worktree add --force "$WORKTREE" 5338338 >/dev/null
    ;;
  pos-modulith|virtual-threads) ;;
  *) echo "fase invalida: $FASE"; exit 1 ;;
esac

export FASE
export LOADTEST_API_CONTEXT="${WORKTREE:+$WORKTREE/}api"
export SPRING_JPA_SHOW_SQL=false
export SPRING_THREADS_VIRTUAL_ENABLED=false
[ "$FASE" = virtual-threads ] && export SPRING_THREADS_VIRTUAL_ENABLED=true
export K6_UID="$(id -u)" K6_GID="$(id -g)"

cd "$REPO_ROOT"
compose() { docker compose -p "$PROJETO" "$@"; }

# Docker 29/compose 5.3 exibem race transitória ("No such container") ao
# criar+iniciar containers em rajada — retry curto contorna sem mascarar
# falhas reais (3 tentativas, 5s entre elas).
retry() {
  local tentativa
  for tentativa in 1 2 3; do
    if "$@"; then return 0; fi
    echo "[loadtest] tentativa $tentativa falhou; repetindo em 5s"
    sleep 5
  done
  return 1
}

cleanup() {
  echo "[loadtest] encerrando..."
  compose down -v --remove-orphans >/dev/null 2>&1 || true
  [ -n "$WORKTREE" ] && git -C "$REPO_ROOT" worktree remove --force "$WORKTREE" >/dev/null 2>&1 || true
}
trap cleanup EXIT

if curl -sf --max-time 2 http://localhost:8080/health >/dev/null 2>&1; then
  echo "[loadtest] FALHA: algo ja ocupa a porta 8080 (dev compose?). Campanhas exigem a maquina quieta."
  exit 1
fi

mkdir -p "$EVIDENCIAS"/{alunos,ranking,write-flow}
rm -rf "${EVIDENCIAS:?}/"*
mkdir -p "$EVIDENCIAS"/{alunos,ranking,write-flow}

echo "[loadtest] fase=$FASE — subindo postgres+api (projeto $PROJETO)"
retry compose --profile loadtest up -d --build postgres api

# JFR de pinning (evento jdk.VirtualThreadPinned, limiar 20ms) — so na fase VT
if [ "$FASE" = virtual-threads ]; then
  echo "[loadtest] iniciando gravacao JFR (pinning)"
  API_CID=$(compose ps -q api)
  until docker exec "$API_CID" jcmd 1 VM.version >/dev/null 2>&1; do sleep 2; done
  docker exec "$API_CID" jcmd 1 JFR.start name=pinning filename=/tmp/pinning.jfr >/dev/null
fi

echo "[loadtest] aguardando api saudavel..."
until [ "$(docker inspect --format '{{.State.Health.Status}}' "$(compose ps -q api)")" = healthy ]; do
  sleep 5
done

echo "[loadtest] subindo metrics + seed"
retry compose --profile loadtest up -d metrics

# Seed como FASE (nao dependencia por cenario): `up <cenario>` reiniciaria o
# servico one-shot ja concluido e o seed (nao idempotente a nivel de INSERT)
# falharia por chave duplicada. Roda uma unica vez, ate completar.
compose --profile loadtest up --exit-code-from seed seed
compose logs --no-color seed 2>&1 | tail -25 > "$EVIDENCIAS/seed-output.txt" || true

# Um `up` por cenario: --exit-code-from implica abort-on-container-exit, que
# abortaria a cadeia inteira na primeira saida (o alunos) — os cenarios
# precisam rodar em comandos separados, em sequencia.
for CENARIO in alunos ranking write-flow; do
  echo "[loadtest] === cenario: $CENARIO ==="
  retry compose --profile loadtest up --exit-code-from "k6-$CENARIO" "k6-$CENARIO"
  compose logs --no-color "k6-$CENARIO" > "$EVIDENCIAS/$CENARIO/k6-console.txt" 2>&1 || true
done
compose stop metrics >/dev/null 2>&1 || true

# ---------- coleta de evidencias ----------
compose logs api > "$EVIDENCIAS/api.log" 2>&1 || true
compose logs seed 2>&1 | tail -25 > "$EVIDENCIAS/seed-output.txt" || true

if [ "$FASE" = virtual-threads ]; then
  API_CID=$(compose ps -q api)
  docker exec "$API_CID" jcmd 1 JFR.stop name=pinning >/dev/null 2>&1 || true
  docker exec "$API_CID" sh -c \
    'jfr print --events jdk.VirtualThreadPinned /tmp/pinning.jfr 2>/dev/null | head -300' \
    > "$EVIDENCIAS/pinning-eventos.txt" || true
fi

{
  echo "date: $(date -Iseconds)"
  echo "harness: docker-compose profile loadtest (projeto $PROJETO)"
  echo "fase: $FASE"
  echo "api-context: ${LOADTEST_API_CONTEXT:-api}"
  echo "virtual-threads: $SPRING_THREADS_VIRTUAL_ENABLED"
  echo "uname: $(uname -srmo) / cpus: $(nproc) / mem: $(free -h | awk '/Mem:/{print $2}')"
  echo "k6: $(docker run --rm grafana/k6:latest version 2>/dev/null)"
  echo "postgres: $(docker exec "$(compose ps -q postgres)" psql -U postgres -tAc 'select version()' 2>/dev/null | head -1)"
  echo "jdk-api: $(docker exec "$(compose ps -q api)" java -version 2>&1 | head -1)"
} > "$EVIDENCIAS/ambiente.txt"

echo "[loadtest] === resumo da fase '$FASE' ==="
for CENARIO in alunos ranking write-flow; do
  echo "--- $CENARIO ---"
  python3 - "$EVIDENCIAS/$CENARIO/resumo.json" <<'PY'
import json, sys
with open(sys.argv[1]) as f:
    m = json.load(f)["metrics"]
dur, reqs, fail = m.get("http_req_duration", {}), m.get("http_reqs", {}), m.get("http_req_failed", {})
# summary-export: 'value' e a taxa; fallback robusto para variants de formato
rate = fail.get("value")
if rate is None:
    p, fl = fail.get("passes", 0), fail.get("fails", 0)
    rate = fl / (p + fl) if (p + fl) else 0.0
print(f"http_reqs: count={reqs.get('count', '?')} rate={reqs.get('rate', 0):.1f}/s")
print(f"duration: avg={dur.get('avg', 0):.1f}ms p(95)={dur.get('p(95)', 0):.1f}ms max={dur.get('max', 0):.1f}ms")
print(f"failed: {rate * 100:.2f}%")
PY
done
echo "[loadtest] evidencias em $EVIDENCIAS"
