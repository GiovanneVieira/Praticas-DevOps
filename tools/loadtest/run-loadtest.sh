#!/usr/bin/env bash
# ============================================================================
# Wrapper da campanha FINAL do Desafio 1 — monólito modular + virtual threads,
# arquitetura declarada no application.properties da API.
#
# Suíte ÚNICA: um processo k6 roda todos os cenários escalonados (startTime no
# suite.ts) — dashboard ESTÁVEL em http://localhost:5665 pela campanha inteira.
# Este wrapper: isola o projeto (-p pratica5-loadtest), VERIFICA (não ativa)
# as virtual threads via jcmd, grava JFR de pinning, semeia a base, agenda os
# marcadores de cenário no arquivo de métricas e coleta as evidências.
#
# Uso: ./run-loadtest.sh
# ============================================================================
set -euo pipefail

FASE=final
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
PROJETO=pratica5-loadtest
EVIDENCIAS="$REPO_ROOT/docs/arquitetura/evidencias/desafio1/$FASE"
# offsets sincronizados com tools/loadtest/config.ts (CENARIOS[].inicioSeg)
MARCADORES="login:0 dashboard:85 ranking:170 write-flow:255 alunos-page:340"

export FASE
export SPRING_JPA_SHOW_SQL=false
export K6_UID="$(id -u)" K6_GID="$(id -g)"

cd "$REPO_ROOT"
compose() { docker compose -p "$PROJETO" "$@"; }

# Docker 29/compose 5.3 exibem race transitória ("No such container") ao
# criar+iniciar containers em rajada — retry curto contorna (3x, 5s).
retry() {
  local tentativa
  for tentativa in 1 2 3; do
    if "$@"; then return 0; fi
    echo "[loadtest] tentativa $tentativa falhou; repetindo em 5s"
    sleep 5
  done
  return 1
}

# jcmd com retry (attach flaky: "Premature EOF" transitorio)
jcmd_retry() {
  local tentativa
  for tentativa in 1 2 3 4 5; do
    if docker exec "$API_CID" jcmd "$@" >/dev/null 2>&1; then return 0; fi
    sleep 2
  done
  return 1
}

cleanup() {
  echo "[loadtest] encerrando..."
  compose down -v --remove-orphans >/dev/null 2>&1 || true
  # docker 29.x: containers de `run` ressuscitam e seguram a network
  docker ps -aq --filter "label=com.docker.compose.project=$PROJETO" | xargs -r docker rm -f >/dev/null 2>&1 || true
  docker network rm "${PROJETO}_default" >/dev/null 2>&1 || true
}
trap cleanup EXIT

# limpeza pre-emptiva: resíduos de execuções interrompidas + containers k6
# zumbis (o Docker 29.x ressuscita containers de binds falhos)
compose down -v --remove-orphans >/dev/null 2>&1 || true
docker ps -aq --filter "label=com.docker.compose.project=$PROJETO" | xargs -r docker rm -f >/dev/null 2>&1 || true
docker network rm "${PROJETO}_default" >/dev/null 2>&1 || true
docker ps -q --filter "label=com.docker.compose.project=$PROJETO" --filter "name=k6" \
  | xargs -r docker rm -f >/dev/null 2>&1 || true

if curl -sf --max-time 2 http://localhost:8080/health >/dev/null 2>&1; then
  echo "[loadtest] FALHA: algo ja ocupa a porta 8080 (dev compose?). Campanhas exigem a maquina quieta."
  exit 1
fi

rm -rf "${EVIDENCIAS:?}/"*
mkdir -p "$EVIDENCIAS/suite"

echo "[loadtest] fase=$FASE (modulith + VT, declarados na API) — subindo postgres+api"
retry compose --profile loadtest up -d --build postgres api

echo "[loadtest] aguardando api saudavel..."
API_CID=$(compose ps -q api)
until [ "$(docker inspect --format '{{.State.Health.Status}}' "$API_CID")" = healthy ]; do
  sleep 5
done

# A app roda como FILHO do maven (dev target: mvn spring-boot:run faz fork) —
# jcmd no PID 1 miraria a JVM do Maven. Resolve o PID real da aplicacao.
APP_PID=$(docker exec "$API_CID" sh -c "jcmd -l | grep gamificacao | cut -d' ' -f1" | tr -d '[:space:]')
if [ -z "$APP_PID" ]; then
  echo "[loadtest] FALHA: processo da aplicacao (com.example...Application) nao encontrado no container."
  exit 1
fi
echo "[loadtest] jvm da aplicacao: pid $APP_PID (pid 1 e o launcher maven)"

# ---------- VERIFICACAO de arquitetura: virtual threads realmente ativas ----------
# A decisão vive no application.properties; a campanha confere o runtime.
# O dump do JDK 21.0.3 omite VTs de request (fora de containers), então o
# sinal canônico é do Tomcat: com VT ativa o executor classico some — threads
# http-nio-8080-exec-* ABSENTES (restam Acceptor/Poller) e
# VirtualThread-unparker PRESENTE (nasce no primeiro uso de VT).
docker exec "$API_CID" grep -q '^spring.threads.virtual.enabled=true' /app/src/main/resources/application.properties \
  || { echo "[loadtest] FALHA: imagem da API desatualizada (properties sem VT). Rode: docker compose -p $PROJETO build --no-cache api"; exit 1; }

docker exec "$API_CID" sh -c \
  '(i=0; while [ $i -lt 20000 ]; do wget -q -O /dev/null "http://localhost:8080/ranking?top=1"; i=$((i+1)); done) >/dev/null 2>&1 &'
sleep 2
DUMP_OK=""
for _ in 1 2 3 4 5; do
  if docker exec "$API_CID" jcmd "$APP_PID" Thread.dump_to_file /tmp/vt-check.txt >/dev/null 2>&1; then
    DUMP_OK=1; break
  fi
  sleep 2
done
if [ -z "$DUMP_OK" ]; then
  echo "[loadtest] FALHA: dump de threads nao gerado (jcmd attach falhou apos retries, jvm pid $APP_PID)."
  exit 1
fi
# grep -c devolve exit 1 quando conta 0 — que aqui e o resultado CORRETO:
# o || true dentro do sh -c evita que o set -e mate o script em silencio.
EXEC_CLASSICAS=$(docker exec "$API_CID" sh -c 'grep -c "http-nio-8080-exec" /tmp/vt-check.txt || true' | tr -d ' ')
UNPARKER=$(docker exec "$API_CID" sh -c 'grep -c "VirtualThread-unparker" /tmp/vt-check.txt || true' | tr -d ' ')
echo "[loadtest] VT check: executor classico http-nio-exec=$EXEC_CLASSICAS (0=correto) | VirtualThread-unparker=$UNPARKER (>=1=correto)"
if [ "${EXEC_CLASSICAS:-1}" -ne 0 ] || [ "${UNPARKER:-0}" -eq 0 ]; then
  echo "[loadtest] FALHA: Tomcat nao esta no executor de virtual threads (verificar spring.threads.virtual.enabled no boot da API)."
  exit 1
fi
{
  echo "virtual-threads: ATIVAS (executor classico http-nio-exec ausente; VirtualThread-unparker presente; jvm pid $APP_PID)"
  echo "dump: $(docker exec "$API_CID" sh -c 'grep -cE "^\#" /tmp/vt-check.txt || true') threads no dump"
} > "$EVIDENCIAS/vt-verificacao.txt"

# JFR de pinning (evento jdk.VirtualThreadPinned, limiar 20ms) na JVM da APP.
# Best-effort: se o attach nao pegar, a campanha segue sem pinning (aviso).
echo "[loadtest] iniciando gravacao JFR (pinning, jvm pid $APP_PID)"
if ! jcmd_retry "$APP_PID" JFR.start name=pinning filename=/tmp/pinning.jfr; then
  echo "[loadtest] AVISO: JFR de pinning nao iniciou (attach instavel); seguindo sem pinning."
fi

echo "[loadtest] subindo metrics + seed"
retry compose --profile loadtest up -d metrics
compose --profile loadtest run --rm --no-deps seed 2>&1 | tee "$EVIDENCIAS/seed-run.log" | tail -8

# ---------- SUÍTE única ----------
# Marcadores de cenário agendados no arquivo de métricas (host-side), nos
# mesmos offsets do startTime da suíte (config.ts) — e ecoados no terminal
# para o cenario corrente ficar visivel durante a campanha.
(
  INICIO=$SECONDS
  echo "== suite inicio $(date -Iseconds) ==" >> "$EVIDENCIAS/metricas-actuator.txt"
  for PAR in $MARCADORES; do
    NOME="${PAR%%:*}"; OFFSET="${PAR##*:}"
    ESPERA=$((OFFSET - (SECONDS - INICIO)))
    [ "$ESPERA" -gt 0 ] && sleep "$ESPERA"
    echo "[loadtest] >>> CENARIO CORRENTE: $NOME ($(date +%H:%M:%S))" | tee -a "$EVIDENCIAS/metricas-actuator.txt"
    sleep 75
    echo "== cenario: $NOME fim $(date -Iseconds) ==" >> "$EVIDENCIAS/metricas-actuator.txt"
  done
) &
MARC_PID=$!

echo "[loadtest] === suite (5 cenarios por taxa-alvo) — dashboard: http://localhost:5665 ==="
# tee: barras de progresso do k6 (uma por cenario ativo) ao vivo no terminal
# e gravadas no arquivo; pipefail (global) propaga falha do compose pro retry.
retry compose --profile loadtest run --rm --no-deps --service-ports k6-suite \
  2>&1 | tee "$EVIDENCIAS/suite/k6-console.txt" \
  || { kill "$MARC_PID" 2>/dev/null; tail -30 "$EVIDENCIAS/suite/k6-console.txt"; exit 1; }
kill "$MARC_PID" 2>/dev/null || true
tail -25 "$EVIDENCIAS/suite/k6-console.txt"
compose stop metrics >/dev/null 2>&1 || true

# ---------- coleta de evidencias ----------
compose logs api > "$EVIDENCIAS/api.log" 2>&1 || true

API_CID=$(compose ps -q api)
APP_PID=$(docker exec "$API_CID" sh -c "jcmd -l | grep gamificacao | cut -d' ' -f1" | tr -d '[:space:]')
jcmd_retry "$APP_PID" JFR.stop name=pinning || true
docker exec "$API_CID" sh -c \
  "jfr print --events jdk.VirtualThreadPinned /tmp/pinning.jfr 2>/dev/null | head -300" \
  > "$EVIDENCIAS/pinning-eventos.txt" || true

{
  echo "date: $(date -Iseconds)"
  echo "harness: docker-compose profile loadtest (projeto $PROJETO) — suite unica (dashboard estavel :5665)"
  echo "arquitetura: modulith + virtual threads + pool 50 (declarados no application.properties da API)"
  echo "verificacao-vt: $(head -1 "$EVIDENCIAS/vt-verificacao.txt" 2>/dev/null)"
  echo "executor: ramping-arrival-rate (taxas-alvo: login 333 | dashboard 5000 | ranking 10000 | write-flow 300 | alunos-page 10000)"
  echo "uname: $(uname -srmo) / cpus: $(nproc) / mem: $(free -h | awk '/Mem:/{print $2}')"
  echo "k6: $(docker run --rm grafana/k6:latest version 2>/dev/null)"
  echo "postgres: $(docker exec "$(compose ps -q postgres)" psql -U postgres -tAc 'select version()' 2>/dev/null | head -1)"
  echo "jdk-api: $(docker exec "$API_CID" java -version 2>&1 | head -1)"
} > "$EVIDENCIAS/ambiente.txt"

# ---------- resumo por cenario (a partir do resultado.json taggeado) ----------
echo "[loadtest] === resumo por cenario (sustentou a taxa-alvo?) ==="
python3 - "$EVIDENCIAS/suite/resultado.json" <<'PY'
import json, sys
from collections import defaultdict

dur, fail, drops = defaultdict(list), defaultdict(lambda: [0, 0]), defaultdict(int)
cenarios = set()
with open(sys.argv[1]) as f:
    for linha in f:
        try:
            e = json.loads(linha)
        except json.JSONDecodeError:
            continue
        if e.get("type") != "Point":
            continue
        d, m = e.get("data", {}), e.get("metric", "")
        c = d.get("tags", {}).get("scenario", "?")
        if m == "http_req_duration":
            dur[c].append(d.get("value", 0)); cenarios.add(c)
        elif m == "http_req_failed":
            # [nao-ok, ok]
            fail[c][0 if d.get("value") else 1] += 1; cenarios.add(c)
        elif m == "dropped_iterations":
            drops[c] += int(d.get("value", 0))

print(f"{'cenario':28} {'reqs':>9} {'p95(ms)':>9} {'max(ms)':>9} {'falhas':>7} {'drops':>6}")
for c in sorted(cenarios):
    vals = sorted(dur[c])
    if not vals: continue
    p95 = vals[int(len(vals) * 0.95)] if len(vals) > 1 else vals[0]
    nok, ok = fail[c]
    taxa = 100 * nok / (ok + nok) if (ok + nok) else 0
    print(f"{c:28} {len(vals):>9} {p95:>9.0f} {vals[-1]:>9.0f} {taxa:>6.1f}% {drops[c]:>6}")
PY
echo "[loadtest] evidencias em $EVIDENCIAS"
