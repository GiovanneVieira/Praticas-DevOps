import { useEffect, useState } from 'react'
import { PAGE_KEY, type Page } from '../types'

/**
 * Navegacao por estado de pagina persistida: as quatro telas sao empilhadas
 * (nao ha rotas), mas o encapsulamento aqui esconde o localStorage dos
 * componentes — trocar por um router no futuro muda um arquivo, nao a app.
 */
export function usePage() {
  const [page, setPage] = useState<Page>(() => (localStorage.getItem(PAGE_KEY) as Page) || 'home')

  useEffect(() => {
    localStorage.setItem(PAGE_KEY, page)
  }, [page])

  return [page, setPage] as const
}
