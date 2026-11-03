import http from 'k6/http'
import { check } from 'k6'
import { BASE_URL, uuidDoAluno } from './config.ts'

/**
 * Módulos aluno/matricula/voucher/conquistas no caminho de leitura — o
 * "refresh do dashboard": 4 GETs em paralelo (http.batch), exatamente como o
 * client carrega a tela. Tags por request mantêm a atribuição de gargalo por
 * endpoint nos relatórios.
 */
export function cenarioDashboard(): void {
  const indice = ((__VU * 1_000_000 + __ITER) % 10_000) + 1
  const alunoId = uuidDoAluno(indice)

  const respostas = http.batch([
    ['GET', `${BASE_URL}/alunos/${alunoId}`, undefined, { tags: { name: 'GET /alunos/{id}' } }],
    ['GET', `${BASE_URL}/alunos/${alunoId}/matriculas`, undefined, { tags: { name: 'GET /alunos/{id}/matriculas' } }],
    ['GET', `${BASE_URL}/alunos/${alunoId}/vouchers`, undefined, { tags: { name: 'GET /alunos/{id}/vouchers' } }],
    ['GET', `${BASE_URL}/alunos/${alunoId}/conquistas`, undefined, { tags: { name: 'GET /alunos/{id}/conquistas' } }],
  ])

  check(respostas[0], { 'aluno 200': (r) => r.status === 200 })
  check(respostas[1], { 'matriculas 200': (r) => r.status === 200 })
  check(respostas[2], { 'vouchers 200': (r) => r.status === 200 })
  check(respostas[3], { 'conquistas 200': (r) => r.status === 200 })
}
