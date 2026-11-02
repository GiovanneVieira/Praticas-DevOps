import type { AlunoResponseDTO } from '../../services/api'

/** Painel de perfil (AC1): dados persistentes do aluno. */
export function ProfilePanel({ aluno }: { aluno: AlunoResponseDTO }) {
  const plan = aluno.plano || 'BASICO'
  const ra = aluno.ra || 'Não informado'

  return (
    <div className="panel profile-panel">
      <div className="panel-head"><div><span>PERFIL</span><h2>Seus dados</h2></div><span className="plan-badge">{plan}</span></div>
      <div className="profile-main"><div className="big-avatar">{aluno.name?.charAt(0).toUpperCase() || 'A'}</div><div><h3>{aluno.name || 'Carregando...'}</h3><p>Aluno LevelUp</p></div></div>
      <div className="data-row"><span>RA</span><b>{ra}</b></div>
      <div className="data-row"><span>Plano</span><b>{plan}</b></div>
      <div className="data-row"><span>Moedas</span><b>{aluno.moedas ?? 0}</b></div>
      <div className="data-row"><span>ID</span><b className="id-value">{aluno.id || '—'}</b></div>
    </div>
  )
}
