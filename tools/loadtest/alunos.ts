import http from 'k6/http'
import { check } from 'k6'
import { BASE_URL, rampaDeCarga } from './config'

export const options = rampaDeCarga

/**
 * Cenario 1 do Desafio 1: listagem de alunos SEM paginacao.
 * Achado esperado: resposta nao delimitada (~1,4 MB com 10k alunos) —
 * o custo cresce linearmente com a base; evidencia para "paginacao futura".
 */
export default function (): void {
  const resposta = http.get(`${BASE_URL}/alunos`)
  check(resposta, {
    'status 200': (r) => r.status === 200,
  })
}
