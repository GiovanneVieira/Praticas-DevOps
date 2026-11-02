import type { ConquistaDTO } from '../../services/api'
import { Icon } from '../ui/Icon'

/**
 * RF02 (Desafio 1): catalogo completo de conquistas com o estado do aluno —
 * desbloqueadas com data, bloqueadas com progresso atual/alvo e a descricao
 * de como obter (a listagem tambem serve de guia).
 */
export function ConquistasGrid({ conquistas, carregando, erro }: { conquistas: ConquistaDTO[], carregando: boolean, erro: string }) {
  const desbloqueadas = conquistas.filter(c => c.desbloqueada).length

  return (
    <section className="panel conquista-panel">
      <div className="panel-head">
        <div><span>RF02</span><h2>Conquistas</h2></div>
        <span className="live-label">{desbloqueadas}/{conquistas.length} DESBLOQUEADAS</span>
      </div>

      {erro && <div className="error-box">{erro}</div>}
      {carregando && <p className="loading-note">Carregando conquistas...</p>}

      {!carregando && !erro && (
        <div className="conquista-grid">
          {conquistas.map(conquista => {
            const progresso = conquista.desbloqueada || conquista.progressoAtual === null || conquista.progressoAlvo === null
              ? null
              : Math.round((conquista.progressoAtual / conquista.progressoAlvo) * 100)
            return (
              <article className={`conquista-card${conquista.desbloqueada ? ' desbloqueada' : ''}`} key={conquista.id}>
                <span className="conquista-icon"><Icon name={conquista.desbloqueada ? 'spark' : 'lock'} /></span>
                <b>{conquista.titulo}</b>
                <small>{conquista.descricao}</small>
                {conquista.desbloqueada ? (
                  <span className="conquista-data">
                    Desbloqueada em {conquista.concedidaEm ? new Date(conquista.concedidaEm).toLocaleDateString('pt-BR') : '—'}
                  </span>
                ) : progresso !== null ? (
                  <>
                    <div className="progress conquista-progresso"><i style={{ width: `${progresso}%` }} /></div>
                    <span className="conquista-progresso-texto">{conquista.progressoAtual}/{conquista.progressoAlvo}</span>
                  </>
                ) : (
                  <span className="conquista-progresso-texto">Alcance o plano Premium</span>
                )}
              </article>
            )
          })}
        </div>
      )}
    </section>
  )
}
