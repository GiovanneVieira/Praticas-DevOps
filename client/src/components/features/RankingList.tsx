import type { RankingEntryDTO } from '../../services/api'
import { Icon } from '../ui/Icon'

interface RankingListProps {
  ranking: RankingEntryDTO[]
  carregando: boolean
  erro: string
  aberto: boolean
  alunoAtualId: string | undefined
  onAbrir: () => void
  onFechar: () => void
  onRecarregar: () => void
}

/**
 * RF01 (Desafio 1): top-20 do ranking. Leitura sob demanda — o painel fica
 * fechado por padrao para nao entrar no ciclo de carga do dashboard.
 */
export function RankingList({ ranking, carregando, erro, aberto, alunoAtualId, onAbrir, onFechar, onRecarregar }: RankingListProps) {
  return (
    <section className="panel ranking-panel">
      <div className="panel-head">
        <div><span>PLATAFORMA</span><h2>Ranking</h2></div>
        {aberto ? (
          <button type="button" className="ranking-toggle" onClick={onFechar}>Ocultar <Icon name="close" /></button>
        ) : (
          <button type="button" className="ranking-toggle" onClick={onAbrir}>Ver top 20 <Icon name="trophy" /></button>
        )}
      </div>

      {!aberto && (
        <p className="loading-note">Top 20 alunos por moedas (desempate por cursos concluídos), servido do read model dedicado.</p>
      )}

      {aberto && (
        <>
          {erro && <div className="error-box">{erro}</div>}
          {carregando && <p className="loading-note">Carregando ranking...</p>}
          {!carregando && ranking.length === 0 && !erro && (
            <p className="loading-note">Ninguém no ranking ainda — conclua um curso com nota superior a 7,0 para entrar.</p>
          )}
          <div className="ranking-list">
            {ranking.map(entry => (
              <div className={`ranking-row${entry.alunoId === alunoAtualId ? ' atual' : ''}`} key={entry.alunoId}>
                <span className={`ranking-posicao p${entry.posicao <= 3 ? entry.posicao : 'n'}`}>{entry.posicao}º</span>
                <div className="ranking-info">
                  <b>{entry.nome}{entry.alunoId === alunoAtualId ? ' · você' : ''}</b>
                  <small>{entry.cursosConcluidos} curso{entry.cursosConcluidos === 1 ? '' : 's'} concluído{entry.cursosConcluidos === 1 ? '' : 's'}</small>
                </div>
                <span className="ranking-moedas"><Icon name="coin" /> {entry.moedas.toLocaleString('pt-BR')}</span>
              </div>
            ))}
          </div>
          <button type="button" className="secondary-button ranking-refresh" onClick={onRecarregar} disabled={carregando}>
            {carregando ? 'Atualizando...' : 'Atualizar ranking'}
          </button>
        </>
      )}
    </section>
  )
}
