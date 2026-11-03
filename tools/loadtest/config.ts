import type { Options } from 'k6/options'

/**
 * Configuração da SUÍTE final do Desafio 1 — monólito modular + virtual
 * threads (declarados no application.properties da API).
 *
 * Um único processo k6 roda TODOS os cenários, escalonados por startTime:
 * dashboard estável em http://localhost:5665 pela campanha inteira, com cada
 * cenário rotulado e timeline própria; resultado.json carrega tags por
 * cenário para análise individual.
 *
 * Executor ramping-arrival-rate: DEMANDA definida em iterações/segundo —
 * a pergunta é "sustenta a taxa-alvo com p(95)<500ms e <1% de erros?".
 * Iterações dropadas = taxa não sustentada = achado de capacidade.
 *
 * Thresholds são critérios de DIAGNÓSTICO — cruzar limite é achado, não
 * falha de build (o compose converte a saída 99 do k6 em sucesso).
 */
const FASE = __ENV.FASE || 'final'

interface CenarioDaSuite {
  nome: string
  exec: string
  taxaAlvo: number
  vusMax: number
  inicioSeg: number
}

// Ordem, taxas e offsets da suíte. login com moldura de burst matinal
// (BCrypt e lento por design — o valor do cenário e o teto por instância);
// leitura indexada e listagem paginada com ambição de teto real sob VT+pool50.
const CENARIOS: CenarioDaSuite[] = [
  { nome: 'login',       exec: 'cenarioLogin',      taxaAlvo: 333,   vusMax: 500,  inicioSeg: 0 },
  { nome: 'dashboard',   exec: 'cenarioDashboard',   taxaAlvo: 5000, vusMax: 800,  inicioSeg: 85 },
  { nome: 'ranking',     exec: 'cenarioRanking',     taxaAlvo: 15000, vusMax: 1200, inicioSeg: 170 },
  { nome: 'write-flow',  exec: 'cenarioWriteFlow',   taxaAlvo: 300,   vusMax: 1200, inicioSeg: 255 },
  { nome: 'alunos-page', exec: 'cenarioAlunosPage',  taxaAlvo: 10000, vusMax: 500,  inicioSeg: 340 },
]

// duração de cada cenário — manter sincronizado com os marcadores do wrapper
export const DURACAO_CENARIO_SEG = 75
export const OFFSETS_CENARIOS = CENARIOS.map((c) => ({ nome: c.nome, inicioSeg: c.inicioSeg }))

export function suiteFinal(): Options {
  const scenarios: Options['scenarios'] = {}
  for (const c of CENARIOS) {
    scenarios[`${FASE}-${c.nome}`] = {
      executor: 'ramping-arrival-rate',
      exec: c.exec,
      startTime: `${c.inicioSeg}s`,
      preAllocatedVUs: Math.min(c.vusMax, 50),
      maxVUs: c.vusMax,
      stages: [
        { target: c.taxaAlvo, duration: '20s' }, // rampa até a taxa-alvo
        { target: c.taxaAlvo, duration: '45s' }, // platô: percentuais estáveis
        { target: 0, duration: '10s' },         // descida
      ],
    }
  }
  return {
    scenarios,
    thresholds: {
      http_req_duration: ['p(95)<500'],
      http_req_failed: ['rate<0.01'],
    },
    tags: { fase: FASE },
  }
}

export const BASE_URL = __ENV.BASE_URL || 'http://api:8080'

export const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } }

/**
 * IDs determinísticos do seed: 0000a000-0000-4000-8000-<seq em 12 dígitos>.
 * Permite que o k6 enderece qualquer um dos 10k alunos sem consulta prévia.
 */
export function uuidDoAluno(indice: number): string {
  return `0000a000-0000-4000-8000-${String(indice).padStart(12, '0')}`
}
