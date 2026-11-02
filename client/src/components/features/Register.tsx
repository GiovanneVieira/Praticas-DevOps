import { useAuth } from '../../context/AuthContext'
import { Icon } from '../ui/Icon'

/** Formulario de cadastro; a decisao de navegar apos sucesso e da pagina. */
export function Register({ onSucesso, onTrocarParaLogin }: { onSucesso: () => void, onTrocarParaLogin: () => void }) {
  const { registrar, carregando, erro, limparErro } = useAuth()

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    limparErro()
    // Referencia capturada antes do await: event.currentTarget e anulado pelo
    // React apos o trecho sincrono do handler (causava TypeError no reset).
    const formEl = event.currentTarget
    const form = new FormData(formEl)
    const name = String(form.get('name') || '').trim()
    const email = String(form.get('email') || '').trim()
    const password = String(form.get('password') || '')

    if (password.length < 6) return

    if (await registrar({ name, email, password })) {
      formEl.reset()
      onSucesso()
    }
  }

  return (
    <main className="register-page">
      <div className="register-intro">
        <span className="eyebrow"><Icon name="spark" /> PRIMEIRO PASSO</span>
        <h1>Comece sua<br /><em>jornada.</em></h1>
        <p>Crie sua conta e entre em uma experiência de estudos onde cada avanço conta.</p>
        <div className="benefit-list">
          <div><span><Icon name="check" /></span><p><b>Acompanhe seu progresso</b><small>Veja sua evolução em um só lugar.</small></p></div>
          <div><span><Icon name="check" /></span><p><b>Conquiste recompensas</b><small>Transforme seus estudos em conquistas.</small></p></div>
          <div><span><Icon name="check" /></span><p><b>Desbloqueie novos desafios</b><small>Continue evoluindo a cada etapa.</small></p></div>
        </div>
      </div>

      <form className="register-card" onSubmit={handleSubmit}>
        <div className="card-heading"><span>CRIAR CONTA</span><h2>Vamos começar.</h2><p>Preencha seus dados para criar seu perfil.</p></div>
        <label>Nome completo<input name="name" type="text" placeholder="Como podemos te chamar?" required /></label>
        <label>E-mail<input name="email" type="email" placeholder="voce@email.com" required /></label>
        <label>Senha<input name="password" type="password" placeholder="Mínimo de 6 caracteres" minLength={6} required /></label>
        {erro && <div className="error-box">{erro}</div>}
        <button className="primary-button full" disabled={carregando}>{carregando ? 'Criando conta...' : 'Criar minha conta'} {!carregando && <Icon name="arrow" />}</button>
        <p className="form-note"><Icon name="lock" /> Seus dados são enviados diretamente para a API.</p>
        <button type="button" className="back-link" onClick={onTrocarParaLogin}>Já tem conta? Entrar</button>
      </form>
    </main>
  )
}
