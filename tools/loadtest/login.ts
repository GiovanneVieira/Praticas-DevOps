import http from 'k6/http'
import { check } from 'k6'
import { BASE_URL, JSON_HEADERS, rampaDeCarga, uuidDoAluno } from './config.ts'

export const options = rampaDeCarga

const SENHA = __ENV.SENHA_CARGA || '123456'

/**
 * Cobertura do modulo aluno — POST /login: dimensao CPU-bound (BCrypt ~100ms
 * por verificacao), comportamento de saturacao distinto dos endpoints
 * I/O-bound. Usa os alunos do seed (hash BCrypt real).
 */
export default function (): void {
  const indice = ((__VU * 1_000_000 + __ITER) % 10_000) + 1
  const alunoId = uuidDoAluno(indice)

  const resposta = http.post(
    `${BASE_URL}/login`,
    JSON.stringify({ email: `aluno.carga.${indice}@gamificacao.local`, password: SENHA }),
    { ...JSON_HEADERS, tags: { name: 'POST /login' } },
  )
  // o corpo guarda o id do aluno logado para o cenario dashboard
  check(resposta, {
    'login 200': (r) => r.status === 200,
    'aluno correto': (r) => r.status === 200 && r.json('id') === alunoId,
  })
}
