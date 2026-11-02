import { useEffect, type ReactNode } from 'react'

/** Base de modais: overlay com fechar-no-clique, ESC e trava de scroll. */
export function Modal({ aberto, onFechar, children }: { aberto: boolean, onFechar: () => void, children: ReactNode }) {
  useEffect(() => {
    if (!aberto) return
    const aoPressionarTecla = (evento: KeyboardEvent) => {
      if (evento.key === 'Escape') onFechar()
    }
    window.addEventListener('keydown', aoPressionarTecla)
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', aoPressionarTecla)
      document.body.style.overflow = ''
    }
  }, [aberto, onFechar])

  if (!aberto) return null

  return (
    <div className="modal-overlay" onClick={onFechar}>
      <div className="modal-card" role="dialog" aria-modal="true" onClick={(evento) => evento.stopPropagation()}>
        {children}
      </div>
    </div>
  )
}
