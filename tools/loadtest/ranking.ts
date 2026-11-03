import http from 'k6/http'
import { check } from 'k6'
import { BASE_URL } from './config.ts'

/**
 * Módulo ranking (RF01) — leitura pesada do read model com índice composto:
 * index-scan em ranking_entries, sem tocar a tabela de alunos.
 */
export function cenarioRanking(): void {
  const resposta = http.get(`${BASE_URL}/ranking?top=20`, { tags: { name: 'GET /ranking?top=20' } })
  const entradas = resposta.json() as Array<{ posicao: number }>
  check(resposta, {
    'status 200': (r) => r.status === 200,
    '20 entradas': () => entradas.length === 20,
  })
}
