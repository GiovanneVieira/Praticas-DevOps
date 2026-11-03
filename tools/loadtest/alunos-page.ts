import http from 'k6/http'
import { check } from 'k6'
import { BASE_URL, rampaDeCarga } from './config.ts'

export const options = rampaDeCarga

/**
 * Validacao da paginacao de GET /alunos (correcao executada a partir da
 * evidencia de carga): janela de 20 sobre a base de 10k — o antes/depois
 * (~60 rps, p95 3-5s, resposta ~1,4MB) quantifica o ganho da correcao.
 */
export default function (): void {
  const resposta = http.get(`${BASE_URL}/alunos?pagina=0&tamanho=20`)
  const pagina = resposta.json('conteudo') as unknown[]
  check(resposta, {
    'status 200': (r) => r.status === 200,
    '20 itens': () => pagina?.length === 20,
  })
}
