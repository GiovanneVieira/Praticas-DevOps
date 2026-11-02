import { useCallback, useEffect, useState } from 'react'
import {
  concluirCurso, desistirMatricula, listarMatriculas, matricular,
  type ConcluirCursoResponseDTO, type MatriculaResponseDTO,
} from '../services/api'

/**
 * Estado e acoes do painel de matriculas (AC1): carregamento inicial com
 * cancelamento, matricular/concluir/desistir e os modais de resultado
 * (reprovacao), recompensa (aprovacao) e confirmacao de desistencia.
 * Extraido do Dashboard, que antes concentrava 7 useStates e 4 handlers.
 */
export function useMatriculas(alunoId: string | undefined, onDadosAtualizados: () => void) {
  const [matriculas, setMatriculas] = useState<MatriculaResponseDTO[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [resultado, setResultado] = useState<ConcluirCursoResponseDTO | null>(null)
  const [recompensa, setRecompensa] = useState<ConcluirCursoResponseDTO | null>(null)
  const [confirmacao, setConfirmacao] = useState<MatriculaResponseDTO | null>(null)

  const carregar = useCallback(async () => {
    if (!alunoId) return
    try {
      setMatriculas(await listarMatriculas(alunoId))
      setErro('')
    } catch (error) {
      setErro(error instanceof Error ? error.message : 'Erro ao carregar matrículas.')
    } finally {
      setCarregando(false)
    }
  }, [alunoId])

  useEffect(() => {
    if (!alunoId) return
    let cancelado = false
    listarMatriculas(alunoId)
      .then((lista) => {
        if (cancelado) return
        setMatriculas(lista)
        setErro('')
      })
      .catch((error) => {
        if (!cancelado) setErro(error instanceof Error ? error.message : 'Erro ao carregar matrículas.')
      })
      .finally(() => { if (!cancelado) setCarregando(false) })
    return () => { cancelado = true }
  }, [alunoId])

  const matricularEmCurso = useCallback(async (nomeCurso: string) => {
    if (!alunoId || !nomeCurso) return
    setErro('')
    try {
      setEnviando(true)
      await matricular(alunoId, nomeCurso)
      await carregar()
    } catch (error) {
      setErro(error instanceof Error ? error.message : 'Não foi possível criar a matrícula.')
    } finally {
      setEnviando(false)
    }
  }, [alunoId, carregar])

  const concluir = useCallback(async (matriculaId: string, notaTexto: string) => {
    if (!alunoId) return
    setErro('')
    setResultado(null)
    setRecompensa(null)
    const notaFinal = Number(notaTexto)
    if (Number.isNaN(notaFinal) || notaFinal < 0 || notaFinal > 10) {
      setErro('Informe uma nota entre 0 e 10.')
      return
    }

    try {
      setEnviando(true)
      const resposta = await concluirCurso(alunoId, matriculaId, notaFinal)
      if (resposta.status === 'CONCLUIDO') {
        setRecompensa(resposta)
      } else {
        setResultado(resposta)
      }
      await carregar()
      onDadosAtualizados()
    } catch (error) {
      setErro(error instanceof Error ? error.message : 'Não foi possível concluir o curso.')
    } finally {
      setEnviando(false)
    }
  }, [alunoId, carregar, onDadosAtualizados])

  const pedirDesistencia = useCallback((matricula: MatriculaResponseDTO) => {
    setErro('')
    setConfirmacao(matricula)
  }, [])

  const confirmarDesistencia = useCallback(async () => {
    if (!alunoId || !confirmacao) return
    const emAndamento = confirmacao.status === 'INICIADO'
    try {
      setEnviando(true)
      await desistirMatricula(alunoId, confirmacao.id)
      setConfirmacao(null)
      setResultado(null)
      await carregar()
    } catch (error) {
      setErro(error instanceof Error
        ? error.message
        : `Não foi possível ${emAndamento ? 'desistir' : 'apagar'} o curso.`)
    } finally {
      setEnviando(false)
    }
  }, [alunoId, confirmacao, carregar])

  return {
    matriculas, carregando, erro, enviando, resultado, recompensa, confirmacao,
    matricularEmCurso, concluir, pedirDesistencia, confirmarDesistencia,
    fecharResultado: () => setResultado(null),
    fecharRecompensa: () => setRecompensa(null),
    cancelarDesistencia: () => setConfirmacao(null),
  }
}
