import type { Options } from 'k6/options'

/**
 * Configuração compartilhada dos cenários de carga do Desafio 1.
 * Fonte única do protocolo: rampa 10 -> 200 VUs (30s), platô de 2m,
 * descida de 30s. Thresholds são critérios de DIAGNÓSTICO — cruzar
 * p(95)<500ms ou rate<1% é achado de evidência, não falha de build
 * (o compose converte a saída 99 do k6 em sucesso).
 */
export const rampaDeCarga: Options = {
  scenarios: {
    ramp: {
      executor: 'ramping-vus',
      startVUs: 10,
      stages: [
        { duration: '30s', target: 200 },
        { duration: '2m', target: 200 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
  },
}

export const BASE_URL = __ENV.BASE_URL || 'http://api:8080'

export const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } }

/**
 * IDs determinísticos do seed: 0000a000-0000-4000-8000-<seq em 12 dígitos>.
 * Permite que o k6 enderece qualquer um dos 10k alunos sem consulta prévia.
 */
export function uuidDoAluno(indice: number): string {
  return `0000a000-0000-4000-8000-${String(indice).padStart(12, '0')}`
}
