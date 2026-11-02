// Cenario 1 do Desafio 1: leitura pesada do ranking (RF01).
// Read model dedicado - espera-se index-scan no indice composto.
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
  const resposta = http.get(`${BASE}/ranking?top=20`);
  check(resposta, {
    'status 200': (r) => r.status === 200,
    '20 entradas': (r) => r.json().length === 20,
  });
}
