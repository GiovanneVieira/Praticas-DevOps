import http from 'k6/http'
import { check } from 'k6'
import { BASE_URL, JSON_HEADERS, uuidDoAluno } from './config.ts'

/**
 * Fluxo crítico de escrita: cada iteração matricula e conclui um curso de um
 * aluno seedado — matricula -> conclusao -> progressao -> recompensas ->
 * evento -> listeners (ranking + conquistas). Tags de nome normalizam a URL
 * dinamica (UUID por aluno) — sem elas, uma serie por aluno (10k grupos).
 */
export function cenarioWriteFlow(): void {
  const indice = ((__VU * 1_000_000 + __ITER) % 10_000) + 1
  const alunoId = uuidDoAluno(indice)

  const matricula = http.post(
    `${BASE_URL}/alunos/${alunoId}/matriculas`,
    JSON.stringify({ nomeCurso: `Curso Carga VU${__VU} It${__ITER}` }),
    { ...JSON_HEADERS, tags: { name: 'POST /alunos/{id}/matriculas' } },
  )
  check(matricula, { 'matricula 201': (r) => r.status === 201 })

  if (matricula.status === 201) {
    const conclusao = http.patch(
      `${BASE_URL}/alunos/${alunoId}/matriculas/${matricula.json('id')}/conclusao`,
      JSON.stringify({ notaFinal: 9.0 }),
      { ...JSON_HEADERS, tags: { name: 'PATCH /alunos/{id}/matriculas/{mid}/conclusao' } },
    )
    check(conclusao, { 'conclusao 200': (r) => r.status === 200 })
  }
}
