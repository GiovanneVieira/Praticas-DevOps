/* eslint-disable react-refresh/only-export-components -- arquivo de contexto:
   expor o hook useAuth junto do Provider e a praxe; fast-refresh local
   perde a invalidacao deste modulo apenas. */
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { createAluno, getAlunoById, login, type AlunoResponseDTO, type CreateAlunoRequest, type LoginRequest } from '../services/api'
import { ALUNO_ID_KEY } from '../types'

/**
 * Sessao da aplicacao em um unico lugar: antes, o id do aluno vivia solto no
 * localStorage e era lido por App/Dashboard em paralelo (estado duplicado e
 * sincronizacao manual). O contexto e a unica fonte de verdade — persiste o
 * id, hidrata o aluno na inicializacao e expoe as acoes de sessao.
 */
interface AuthContextValue {
  aluno: AlunoResponseDTO | null
  carregando: boolean
  erro: string
  registrar: (dados: CreateAlunoRequest) => Promise<boolean>
  entrar: (dados: LoginRequest) => Promise<boolean>
  sair: () => void
  recarregar: () => Promise<void>
  limparErro: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [aluno, setAluno] = useState<AlunoResponseDTO | null>(null)
  // Carregando apenas quando ha sessao salva para hidratar; sem id, nada a
  // esperar (evita setState sincrono no corpo do effect).
  const [carregando, setCarregando] = useState(() => Boolean(localStorage.getItem(ALUNO_ID_KEY)))
  const [erro, setErro] = useState('')

  useEffect(() => {
    const idSalvo = localStorage.getItem(ALUNO_ID_KEY)
    if (!idSalvo) return
    getAlunoById(idSalvo)
      .then(setAluno)
      .catch(() => localStorage.removeItem(ALUNO_ID_KEY))
      .finally(() => setCarregando(false))
  }, [])

  const registrar = useCallback(async (dados: CreateAlunoRequest) => {
    setErro('')
    try {
      setCarregando(true)
      const criado = await createAluno(dados)
      setAluno(criado)
      localStorage.setItem(ALUNO_ID_KEY, criado.id)
      return true
    } catch (error) {
      setErro(error instanceof Error ? error.message : 'Não foi possível criar sua conta.')
      return false
    } finally {
      setCarregando(false)
    }
  }, [])

  const entrar = useCallback(async (dados: LoginRequest) => {
    setErro('')
    try {
      setCarregando(true)
      const logado = await login(dados)
      setAluno(logado)
      localStorage.setItem(ALUNO_ID_KEY, logado.id)
      return true
    } catch (error) {
      setErro(error instanceof Error ? error.message : 'Não foi possível entrar.')
      return false
    } finally {
      setCarregando(false)
    }
  }, [])

  const sair = useCallback(() => {
    localStorage.removeItem(ALUNO_ID_KEY)
    setAluno(null)
  }, [])

  const recarregar = useCallback(async () => {
    const idSalvo = localStorage.getItem(ALUNO_ID_KEY)
    if (!idSalvo) return
    try {
      setAluno(await getAlunoById(idSalvo))
    } catch {
      // mantem o estado atual: falha pontual de refresh nao derruba a sessao
    }
  }, [])

  const limparErro = useCallback(() => setErro(''), [])

  const value = useMemo(
    () => ({ aluno, carregando, erro, registrar, entrar, sair, recarregar, limparErro }),
    [aluno, carregando, erro, registrar, entrar, sair, recarregar, limparErro],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const contexto = useContext(AuthContext)
  if (!contexto) throw new Error('useAuth precisa estar dentro de <AuthProvider>.')
  return contexto
}
