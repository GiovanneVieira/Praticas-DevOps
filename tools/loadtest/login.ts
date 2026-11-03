import http from 'k6/http'
import { check } from 'k6'
import { BASE_URL, JSON_HEADERS, uuidDoAluno } from './config.ts'

/**
 * Módulo aluno — POST /login (burst matinal: 100k logins em ~5min ≈ 333/s).
 * BCrypt é lento POR DESIGN (custo de CPU por verificação): o valor do cenário
 * é quantificar o teto por instância (~125/s medidos) e dimensionar réplicas
 * para o burst — não perseguir dezenas de milhares de rps numa função
 * criptográfica.
 */
export function cenarioLogin(): void {
  const indice = ((__VU * 1_000_000 + __ITER) % 10_000) + 1
  const alunoId = uuidDoAluno(indice)

  const resposta = http.post(
    `${BASE_URL}/login`,
    JSON.stringify({ email: `aluno.carga.${indice}@gamificacao.local`, password: '123456' }),
    { ...JSON_HEADERS, tags: { name: 'POST /login' } },
  )
  check(resposta, {
    'login 200': (r) => r.status === 200,
    'aluno correto': (r) => r.status === 200 && r.json('id') === alunoId,
  })
}
