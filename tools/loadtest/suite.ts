import { suiteFinal } from './config.ts'

/**
 * Suíte final do Desafio 1 — TODOS os cenários num único processo k6,
 * escalonados por startTime (ver config.ts): dashboard estável em
 * http://localhost:5665 durante a campanha inteira, cada cenário com rótulo
 * e timeline própria no painel, e resultado.json taggeado por cenário.
 *
 * Os handlers (exec:) são re-exportados dos módulos de cada módulo de domínio.
 */
export const options = suiteFinal()

export { cenarioLogin } from './login.ts'
export { cenarioDashboard } from './dashboard.ts'
export { cenarioRanking } from './ranking.ts'
export { cenarioWriteFlow } from './write-flow.ts'
export { cenarioAlunosPage } from './alunos-page.ts'
