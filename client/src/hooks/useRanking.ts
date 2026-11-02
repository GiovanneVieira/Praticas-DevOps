import { useCallback, useState } from 'react'
import { getRanking, type RankingEntryDTO } from '../services/api'

/**
 * RF01 (Desafio 1): ranking carregado sob demanda — leitura pesada nao entra
 * no ciclo de carga inicial do dashboard; o usuario decide quando abrir.
 */
export function useRanking() {
  const [ranking, setRanking] = useState<RankingEntryDTO[]>([])
  const [carregando, setCarregando] = useState(false)
  const [erro, setErro] = useState('')
  const [aberto, setAberto] = useState(false)

  const carregar = useCallback(async () => {
    setCarregando(true)
    try {
      setRanking(await getRanking(20))
      setErro('')
    } catch (error) {
      setErro(error instanceof Error ? error.message : 'Não foi possível carregar o ranking.')
    } finally {
      setCarregando(false)
    }
  }, [])

  const abrir = useCallback(() => {
    setAberto(true)
    void carregar()
  }, [carregar])

  const fechar = useCallback(() => setAberto(false), [])

  return { ranking, carregando, erro, aberto, abrir, fechar, recarregar: carregar }
}
