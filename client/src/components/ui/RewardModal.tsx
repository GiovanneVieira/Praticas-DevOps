import type { ConcluirCursoResponseDTO } from '../../services/api'
import { Icon } from './Icon'
import { Modal } from './Modal'

export function RewardModal({ resultado, onFechar }: { resultado: ConcluirCursoResponseDTO | null, onFechar: () => void }) {
  if (!resultado) return null
  const virouPremium = resultado.upgradePremium
  // TDD5: a cada 12 cursos concluídos com nota > 7,0 o aluno Premium recebe 3 moedas + voucher de novo
  const recompensaRecorrente = !virouPremium && resultado.voucher !== null
  const temRecompensas = virouPremium || recompensaRecorrente || resultado.cursosLiberados.length > 0

  return (
    <Modal aberto onFechar={onFechar}>
      <div className="reward-hero">
        <span className="reward-hero-icon"><Icon name={virouPremium || recompensaRecorrente ? 'spark' : 'check'} /></span>
        {(virouPremium || recompensaRecorrente) && (
          <span className="reward-eyebrow">{virouPremium ? 'UPGRADE DESBLOQUEADO' : 'A CADA 12 CURSOS'}</span>
        )}
        <h3>
          {virouPremium ? 'Você virou PREMIUM!' : recompensaRecorrente ? 'Recompensa desbloqueada!' : 'Curso concluído!'}
        </h3>
        <p>
          {virouPremium
            ? 'Parabéns! Ao concluir o 12º curso com nota superior a 7,0, sua assinatura foi alterada para o plano Premium.'
            : recompensaRecorrente
              ? 'Mais um ciclo de 12 cursos concluídos com nota superior a 7,0: você recebe novamente moedas e um voucher para projetos reais.'
              : `Nota superior a 7,0 em "${resultado.nomeCurso}". Continue assim para evoluir seu plano.`}
        </p>
      </div>

      {temRecompensas && (
      <div className="reward-rows">
        {resultado.moedasRecebidas > 0 && (
          <div className="reward-row">
            <span className="reward-row-icon coin"><Icon name="coin" /></span>
            <div><b>{resultado.moedasRecebidas} moeda{resultado.moedasRecebidas === 1 ? '' : 's'} recebida{resultado.moedasRecebidas === 1 ? '' : 's'}</b><small>Saldo atual: {(resultado.moedas ?? 0).toLocaleString('pt-BR')}</small></div>
          </div>
        )}
        {resultado.voucher && (
          <div className="reward-row">
            <span className="reward-row-icon ticket"><Icon name="ticket" /></span>
            <div><b>{resultado.voucher.nome}</b><small>R$ {resultado.voucher.valorEmReais.toFixed(2)} · válido até {resultado.voucher.expiresAt ? new Date(resultado.voucher.expiresAt).toLocaleDateString('pt-BR') : '—'}</small></div>
          </div>
        )}
        {resultado.cursosLiberados.length > 0 && (
          <div className="reward-row">
            <span className="reward-row-icon book"><Icon name="book" /></span>
            <div>
              <b>{resultado.cursosLiberados.length} novos cursos liberados</b>
              <small>{resultado.cursosLiberados.map(curso => curso.nomeCurso).join(' · ')}</small>
            </div>
          </div>
        )}
      </div>
      )}

      <div className="modal-actions">
        <button className="primary-button full" onClick={onFechar}>Continuar <Icon name="arrow" /></button>
      </div>
    </Modal>
  )
}
