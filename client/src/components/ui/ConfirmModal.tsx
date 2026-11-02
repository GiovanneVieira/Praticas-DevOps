import type { MatriculaResponseDTO } from '../../services/api'
import { Icon } from './Icon'
import { Modal } from './Modal'

export function ConfirmModal({ aberto, matricula, enviando, onCancelar, onConfirmar }: {
  aberto: boolean
  matricula: MatriculaResponseDTO | null
  enviando: boolean
  onCancelar: () => void
  onConfirmar: () => void
}) {
  if (!matricula) return null
  const emAndamento = matricula.status === 'INICIADO'

  return (
    <Modal aberto={aberto} onFechar={onCancelar}>
      <span className="modal-icon danger"><Icon name={emAndamento ? 'alert' : 'book'} /></span>
      <h3>{emAndamento ? 'Desistir do curso?' : 'Apagar do histórico?'}</h3>
      <p>
        {emAndamento
          ? <>Você vai desistir de <strong>"{matricula.nomeCurso}"</strong>. A matrícula será removida do seu histórico e todo o progresso nela será perdido.</>
          : <>Você vai apagar <strong>"{matricula.nomeCurso}"</strong> do seu histórico. Esta ação não pode ser desfeita.</>}
      </p>
      <div className="modal-actions">
        <button className="secondary-button" onClick={onCancelar} disabled={enviando}>Cancelar</button>
        <button className="danger-button" onClick={onConfirmar} disabled={enviando}>
          {enviando ? 'Removendo...' : emAndamento ? 'Sim, desistir' : 'Sim, apagar'}
        </button>
      </div>
    </Modal>
  )
}
