import { useCallback, useState, type FormEvent } from 'react'
import { useAuth } from '../../context/AuthContext'
import { useConquistas } from '../../hooks/useConquistas'
import { useMatriculas } from '../../hooks/useMatriculas'
import { useRanking } from '../../hooks/useRanking'
import { useVouchers } from '../../hooks/useVouchers'
import { ConfirmModal } from '../ui/ConfirmModal'
import { Icon } from '../ui/Icon'
import { RewardModal } from '../ui/RewardModal'
import { ConquistasGrid } from './ConquistasGrid'
import { CoursesPanel } from './CoursesPanel'
import { ProfilePanel } from './ProfilePanel'
import { RankingList } from './RankingList'
import { VoucherPanel } from './VoucherPanel'

/**
 * Dashboard (AC1 + RF01/RF02 do Desafio 1): composicao dos paineis. Cada
 * pedaco de estado/dados vive no seu hook (useMatriculas, useVouchers,
 * useConquistas, useRanking) — o componente apenas orquestra o refresh
 * conjunto apos acoes que mudam dados da API (ex.: conclusao desbloqueia
 * conquista, credita moedas e pode emitir voucher).
 */
export function Dashboard({ onRegistrar, onEntrar, onSair }: { onRegistrar: () => void, onEntrar: () => void, onSair: () => void }) {
  const { aluno, carregando, recarregar } = useAuth()
  const [versaoDados, setVersaoDados] = useState(0)

  const alunoId = aluno?.id
  const aoDadosAtualizados = useCallback(() => {
    setVersaoDados(versao => versao + 1)
    void recarregar()
  }, [recarregar])

  const matriculas = useMatriculas(alunoId, aoDadosAtualizados)
  const vouchers = useVouchers(alunoId, versaoDados)
  const conquistas = useConquistas(alunoId, versaoDados)
  const ranking = useRanking()

  if (!aluno && !carregando) {
    return <main className="empty-dashboard"><div className="empty-icon"><Icon name="user" /></div><h1>Nenhum perfil encontrado.</h1><p>Faça login ou crie sua conta para acessar seu dashboard.</p><div className="empty-actions"><button className="primary-button" onClick={onEntrar}>Entrar</button><button className="secondary-button" onClick={onRegistrar}>Criar conta</button></div></main>
  }

  if (!aluno) {
    return <main className="empty-dashboard"><p>Carregando perfil...</p></main>
  }

  const handleMatricular = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    void matriculas.matricularEmCurso(String(form.get('nomeCurso') || '').trim())
  }

  const firstName = aluno.name?.split(' ')[0] || 'aluno'
  const plan = aluno.plano || 'BASICO'
  // progresso oficial persistido no aluno: apagar curso do histórico não reduz
  const concluidos = aluno.cursosConcluidos ?? 0
  const iniciados = matriculas.matriculas.filter(m => m.status === 'INICIADO').length

  return <main className="dashboard-page">
    <section className="dashboard-head">
      <div><span className="eyebrow">SEU ESPAÇO</span><h1>Olá, {firstName}.</h1><p>Continue sua jornada. Cada passo conta.</p></div>
      <button className="logout-button" onClick={onSair}><Icon name="logout" /> Sair</button>
    </section>

    <section className="stats-grid">
      <div className="stat-card accent"><span className="stat-icon"><Icon name="book" /></span><div><small>MATRÍCULAS</small><strong>{matriculas.matriculas.length}</strong><span>{iniciados} em andamento</span></div></div>
      <div className="stat-card"><span className="stat-icon"><Icon name="coin" /></span><div><small>MOEDAS</small><strong>{(aluno.moedas ?? 0).toLocaleString('pt-BR')}</strong><span>Saldo disponível</span></div></div>
      <div className="stat-card"><span className="stat-icon"><Icon name="check" /></span><div><small>CURSOS CONCLUÍDOS</small><strong>{concluidos}</strong><span>{12 - concluidos > 0 ? `${12 - concluidos} até o Premium` : 'Meta atingida'}</span></div></div>
      <div className="stat-card"><span className="stat-icon"><Icon name="ticket" /></span><div><small>PLANO</small><strong>{plan}</strong><span>{plan === 'PREMIUM' ? 'Acesso Premium' : 'Plano atual'}</span></div></div>
    </section>

    {matriculas.resultado && (
      <section className="reward-banner failure">
        <span className="reward-icon"><Icon name="close" /></span>
        <div>
          <b>Curso "{matriculas.resultado.nomeCurso}" reprovado (nota ≤ 7,0).</b>
          <small>Nenhum curso liberado e o progresso para o Premium não foi incrementado.</small>
        </div>
      </section>
    )}

    <section className="dashboard-columns">
      <CoursesPanel
        matriculas={matriculas.matriculas}
        carregando={matriculas.carregando}
        erro={matriculas.erro}
        enviando={matriculas.enviando}
        onMatricular={handleMatricular}
        onConcluir={matriculas.concluir}
        onDesistir={matriculas.pedirDesistencia}
      />
      {aluno && <ProfilePanel aluno={aluno} />}
    </section>

    <ConquistasGrid conquistas={conquistas.conquistas} carregando={conquistas.carregando} erro={conquistas.erro} />

    <VoucherPanel vouchers={vouchers.vouchers} />

    <RankingList
      ranking={ranking.ranking}
      carregando={ranking.carregando}
      erro={ranking.erro}
      aberto={ranking.aberto}
      alunoAtualId={alunoId}
      onAbrir={ranking.abrir}
      onFechar={ranking.fechar}
      onRecarregar={ranking.recarregar}
    />

    <ConfirmModal
      aberto={matriculas.confirmacao !== null}
      matricula={matriculas.confirmacao}
      enviando={matriculas.enviando}
      onCancelar={matriculas.cancelarDesistencia}
      onConfirmar={() => void matriculas.confirmarDesistencia()}
    />

    <RewardModal resultado={matriculas.recompensa} onFechar={matriculas.fecharRecompensa} />
  </main>
}
