// Cenario 3 do Desafio 1: fluxo critico de escrita.
// Cada iteracao matricula e conclui um curso de um aluno seedado, exercitando
// o caminho completo: matricula -> conclusao -> progressao -> recompensas ->
// evento -> listeners (ranking + conquistas).
// Alunos enderecados pelo indice deterministico do seed (1..10000).
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
const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

function uuidDoAluno(indice) {
  return `0000a000-0000-4000-8000-${String(indice).padStart(12, '0')}`;
}

export default function () {
  // distribui alunos distintos entre VUs/iteracoes (deterministico)
  const indice = ((__VU * 1000000 + __ITER) % 10000) + 1;
  const alunoId = uuidDoAluno(indice);

  const matricula = http.post(
    `${BASE}/alunos/${alunoId}/matriculas`,
    JSON.stringify({ nomeCurso: `Curso Carga VU${__VU} It${__ITER}` }),
    JSON_HEADERS
  );
  check(matricula, { 'matricula 201': (r) => r.status === 201 });

  if (matricula.status === 201) {
    const conclusao = http.patch(
      `${BASE}/alunos/${alunoId}/matriculas/${matricula.json('id')}/conclusao`,
      JSON.stringify({ notaFinal: 9.0 }),
      JSON_HEADERS
    );
    check(conclusao, { 'conclusao 200': (r) => r.status === 200 });
  }
}
