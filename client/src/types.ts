// Tipos e constantes compartilhados da UI.
// Mantidos em um modulo proprio para que componentes/hooks nao importem
// uns aos outros apenas por constantes (evita acoplamento artificial).

export type Page = 'home' | 'register' | 'login' | 'dashboard'

export const ALUNO_ID_KEY = 'gamificacao-aluno-id'
export const PAGE_KEY = 'gamificacao-page'
