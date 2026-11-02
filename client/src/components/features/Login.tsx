import { useAuth } from '../../context/AuthContext'
import { Icon } from '../ui/Icon'

export function Login({ onSucesso, onTrocarParaRegistro }: { onSucesso: () => void, onTrocarParaRegistro: () => void }) {
  const { entrar, carregando, erro, limparErro } = useAuth()

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    limparErro()
    const formEl = event.currentTarget
    const form = new FormData(formEl)
    const email = String(form.get('email') || '').trim()
    const password = String(form.get('password') || '')

    if (await entrar({ email, password })) {
      formEl.reset()
      onSucesso()
    }
  }

  return (
    <main className="register-page">
      <div className="register-intro">
        <span className="eyebrow"><Icon name="lock" /> BEM-VINDO DE VOLTA</span>
        <h1>Entre na sua<br /><em>conta.</em></h1>
        <p>Acesse seu dashboard, acompanhe suas matrículas e continue de onde parou.</p>
        <div className="benefit-list">
          <div><span><Icon name="check" /></span><p><b>Seu progresso salvo</b><small>Cursos, recompensas e moedas em um só lugar.</small></p></div>
          <div><span><Icon name="check" /></span><p><b>Segurança com BCrypt</b><small>Sua senha é validada contra o hash armazenado.</small></p></div>
        </div>
      </div>

      <form className="register-card" onSubmit={handleSubmit}>
        <div className="card-heading"><span>LOGIN</span><h2>Entrar.</h2><p>Use o e-mail e a senha do seu cadastro.</p></div>
        <label>E-mail<input name="email" type="email" placeholder="voce@email.com" required /></label>
        <label>Senha<input name="password" type="password" placeholder="Sua senha" required /></label>
        {erro && <div className="error-box">{erro}</div>}
        <button className="primary-button full" disabled={carregando}>{carregando ? 'Entrando...' : 'Entrar'} {!carregando && <Icon name="arrow" />}</button>
        <p className="form-note"><Icon name="lock" /> As credenciais são validadas na API (BCrypt).</p>
        <button type="button" className="back-link" onClick={onTrocarParaRegistro}>Não tem conta? Criar agora</button>
      </form>
    </main>
  )
}
