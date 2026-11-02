// Cenario 2 do Desafio 1: listagem de alunos SEM paginacao.
// Achado esperado do mini-lab: resposta nao delimitada - o custo cresce
// linearmente com a base; evidencia para "paginacao futura" no ADR.
import http from 'k6/http';
import { check } from 'k6';

export const options = {
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
    'http_req_duration': ['p(95)<500'],
    'http_req_failed': ['rate<0.01'],
  },
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  const resposta = http.get(`${BASE}/alunos`);
  check(resposta, {
    'status 200': (r) => r.status === 200,
  });
}
