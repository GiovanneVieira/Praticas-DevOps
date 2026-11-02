import { useEffect, useState } from 'react'
import { getConquistas, type ConquistaDTO } from '../services/api'

/**
 * RF02 (Desafio 1): catalogo de conquistas com o estado do aluno. O parametro
 * `versao` forca refresh apos conclusoes — o backend desbloqueia conquistas
 * via evento de conclusao de curso. setState apenas em then-callbacks.
 */
export function useConquistas(alunoId: string | undefined, versao: number) {
  const [conquistas, setConquistas] = useState<ConquistaDTO[]>([])
  const [carregando, setCarregando] = useState(true)
  const [erro, setErro] = useState('')

  useEffect(() => {
    if (!alunoId) return
    let cancelado = false
    getConquistas(alunoId)
      .then((lista) => {
        if (cancelado) return
        setConquistas(lista)
        setErro('')
      })
      .catch((error) => {
        if (!cancelado) setErro(error instanceof Error ? error.message : 'Não foi possível carregar as conquistas.')
      })
      .finally(() => { if (!cancelado) setCarregando(false) })
    return () => { cancelado = true }
  }, [alunoId, versao])

  return { conquistas, carregando, erro }
}
