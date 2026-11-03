import http from 'k6/http'
import { check } from 'k6'
import { BASE_URL, rampaDeCarga } from './config.ts'

export const options = rampaDeCarga('ranking')

/**
 * Cenario 2 do Desafio 1: leitura pesada do ranking (RF01).
 * Read model dedicado com indice composto — espera-se index-scan
 * em ranking_entries, sem tocar a tabela de alunos.
 */
export default function (): void {
  const resposta = http.get(`${BASE_URL}/ranking?top=20`, { tags: { name: 'GET /ranking?top=20' } })
  const entradas = resposta.json() as Array<{ posicao: number }>
  check(resposta, {
    'status 200': (r) => r.status === 200,
    '20 entradas': () => entradas.length === 20,
  })
}
