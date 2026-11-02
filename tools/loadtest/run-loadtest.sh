#!/usr/bin/env bash
# ============================================================================
# Orquestrador das rodadas de carga do Desafio 1.
#
# Uso: ./run-loadtest.sh <fase>
#   fases: baseline | pos-modulith | virtual-threads
#
# Protocolo (identico em todas as fases - so a variavel em teste muda):
#   1. Ambiente resetado (docker compose down -v; sobe apenas o postgres)
#   2. API sobe no HOST via mvnw spring-boot:run (mesmo JDK 21 nas fases)
#      - baseline/pos-modulith: SPRING_JPA_SHOW_SQL=false
#      - virtual-threads: idem + SPRING_THREADS_VIRTUAL_ENABLED=true
#   3. Seed deterministico de 10k alunos (seed.sql)
#   4. Cenarios k6 separados (alunos, ranking, write-flow), rampa 10->200 VUs
#      com captura de metricas Actuator (hikari/tomcat) em paralelo
#   5. Evidencias em docs/arquitetura/evidencias/desafio1/<fase>/
# ============================================================================
set -euo pipefail

FASE=${1:?uso: run-loadtest.sh <baseline|pos-modulith|virtual-threads>}
REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
LOADTEST="$REPO_ROOT/tools/loadtest"
EVIDENCIAS="$REPO_ROOT/docs/arquitetura/evidencias/desafio1/$FASE"
API_DIR="$REPO_ROOT/api"
API_LOG="$EVIDENCIAS/api.log"
API_PID=""

case "$FASE" in
  baseline|pos-modulith) API_ENV=(SPRING_JPA_SHOW_SQL=false) ;;
  virtual-threads)       API_ENV=(SPRING_JPA_SHOW_SQL=false SPRING_THREADS_VIRTUAL_ENABLED=true) ;;
  *) echo "fase invalida: $FASE"; exit 1 ;;
esac

mkdir -p "$EVIDENCIAS"
rm -rf "${EVIDENCIAS:?}/"*
cd "$REPO_ROOT"

cleanup() {
  echo "[loadtest] encerrando..."
  [ -n "$API_PID" ] && kill "$API_PID" 2>/dev/null || true
  pkill -TERM -f 'gamificacao.grupo_7' 2>/dev/null || true
  pkill -TERM -f 'spring-boot:run' 2>/dev/null || true
  fuser -k -TERM 8080/tcp 2>/dev/null || true
  docker compose down --remove-orphans >/dev/null 2>&1 || true
}
trap cleanup EXIT

echo "[loadtest] fase=$FASE — resetando ambiente"
# Garante que nenhum java/mvnw de rodada anterior segurou a porta 8080.
if curl -sf --max-time 2 http://localhost:8080/health >/dev/null 2>&1; then
  echo "[loadtest] porta 8080 ocupada por processo anterior — encerrando antes de seguir"
  pkill -TERM -f 'gamificacao.grupo_7' 2>/dev/null || true
  pkill -TERM -f 'spring-boot:run' 2>/dev/null || true
  fuser -k -TERM 8080/tcp 2>/dev/null || true
  sleep 3
fi
docker compose down -v --remove-orphans >/dev/null 2>&1 || true
docker compose up -d postgres
# Polla o banco de fato: pg_isready responde ao servidor temporario do entrypoint
# antes de POSTGRES_DB existir (race classico de volume novo).
until docker exec devops_postgres psql -U postgres -d devops -tAc 'select 1' >/dev/null 2>&1; do sleep 2; done
echo "[loadtest] postgres pronto"

echo "[loadtest] subindo a API no host (env: ${API_ENV[*]})"
: > "$API_LOG"
( cd "$API_DIR" && exec env "${API_ENV[@]}" \
    DATABASE_URL=jdbc:postgresql://localhost:5433/devops \
    bash mvnw -q spring-boot:run ) > "$API_LOG" 2>&1 &
API_PID=$!
echo "[loadtest] api pid=$API_PID (aguardando /health...)"
for _ in $(seq 1 90); do
  if curl -sf http://localhost:8080/health >/dev/null 2>&1; then break; fi
  if ! kill -0 "$API_PID" 2>/dev/null; then
    echo "[loadtest] FALHA ao subir a API"; tail -30 "$API_LOG"; exit 1
  fi
  sleep 2
done
curl -sf http://localhost:8080/health >/dev/null
# Sanity: o schema do Hibernate precisa existir antes do seed.
for _ in $(seq 1 30); do
  if docker exec devops_postgres psql -U postgres -d devops -tAc \
      "select 1 from information_schema.tables where table_name='alunos'" 2>/dev/null | grep -q 1; then break; fi
  sleep 2
done

echo "[loadtest] semeando base (10k alunos)"
docker exec -i devops_postgres psql -U postgres -d devops -v ON_ERROR_STOP=1 < "$LOADTEST/seed.sql" \
  | tail -5 | tee "$EVIDENCIAS/seed-output.txt"

# registro do ambiente (reproducibilidade da evidencia)
{
  echo "date: $(date -Iseconds)"
  echo "uname: $(uname -srmo)"
  echo "cpus: $(nproc)"
  echo "mem: $(free -h | awk '/Mem:/{print $2}')"
  echo "jdk: $(cd "$API_DIR" && bash mvnw -q help:evaluate -Dexpression=maven.compiler.source -DforceStdout 2>/dev/null || echo '?') (fonte pom)"
  echo "postgres: $(docker exec devops_postgres psql -U postgres -tAc 'select version()')"
  echo "k6: $(docker run --rm grafana/k6 version 2>/dev/null)"
  echo "virtual-threads: $([ "$FASE" = virtual-threads ] && echo true || echo false)"
} > "$EVIDENCIAS/ambiente.txt"

CENARIOS=(alunos ranking write-flow)
DURACAO_APROX=210  # 30s rampa + 2m platô + 30s descida (+ margem p/ captura)

for CENARIO in "${CENARIOS[@]}"; do
  echo "[loadtest] === cenario: $CENARIO ==="
  mkdir -p "$EVIDENCIAS/$CENARIO"
  BASE_URL=http://localhost:8080 "$LOADTEST/capture-metrics.sh" "$DURACAO_APROX" \
    "$EVIDENCIAS/$CENARIO/metricas-actuator.txt" &
  CAP_PID=$!

  # Thresholds cruzados NAO abortam a rodada: sao achados (ex.: /alunos sem
  # paginacao) e precisam constar na evidencia de todos os cenarios.
  docker run --rm --network host \
    --user "$(id -u):$(id -g)" \
    -v "$LOADTEST":/scripts:ro \
    -v "$EVIDENCIAS/$CENARIO":/out \
    grafana/k6 run \
      -o json=/out/resultado.json \
      --summary-export=/out/resumo.json \
      /scripts/$CENARIO.js 2>&1 | tee "$EVIDENCIAS/$CENARIO/k6-console.txt" | tail -20 || true

  sleep 2
  kill "$CAP_PID" 2>/dev/null || true
done

echo "[loadtest] resumo da fase '$FASE' gravado em $EVIDENCIAS"
for CENARIO in "${CENARIOS[@]}"; do
  echo "--- $CENARIO ---"
  python3 - "$EVIDENCIAS/$CENARIO/resumo.json" <<'PY'
import json, sys
with open(sys.argv[1]) as f:
    r = json.load(f)
m = r.get('metrics', {})
dur = m.get('http_req_duration', {})
reqs = m.get('http_reqs', {})
fail = m.get('http_req_failed', {})
print(f"http_reqs: count={reqs.get('count', '?')} rate={reqs.get('rate', '?'):.1f}/s")
print(f"duration: avg={dur.get('avg', 0):.1f}ms p(95)={dur.get('p(95)', 0):.1f}ms max={dur.get('max', 0):.1f}ms")
print(f"failed: {fail.get('rate', 0)*100:.2f}%")
PY
done
