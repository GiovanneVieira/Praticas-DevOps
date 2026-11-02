#!/usr/bin/env bash
# Captura periodica de metricas do Actuator durante as rodadas de carga.
# Uso: ./capture-metrics.sh <duracao_segundos> <arquivo_saida>
set -uo pipefail

DURACAO=${1:?uso: capture-metrics.sh <segundos> <arquivo>}
ARQUIVO=${2:?uso: capture-metrics.sh <segundos> <arquivo>}
BASE=${BASE_URL:-http://localhost:8080}

METRICAS=(
  "hikaricp.connections.active"
  "hikaricp.connections.pending"
  "hikaricp.connections.max"
  "tomcat.threads.busy"
  "tomcat.threads.current"
)

FIM=$((SECONDS + DURACAO))
while [ "$SECONDS" -lt "$FIM" ]; do
  TS=$(date -Iseconds)
  for METRICA in "${METRICAS[@]}"; do
    VALOR=$(curl -sf "$BASE/actuator/metrics/$METRICA" 2>/dev/null \
      | tr -d '\n' || echo 'indisponivel')
    echo "$TS $METRICA $VALOR" >> "$ARQUIVO"
  done
  sleep 5
done
