import { AuthProvider, useAuth } from './context/AuthContext'
import { usePage } from './hooks/usePage'
import { Dashboard } from './components/features/Dashboard'
import { Home } from './components/features/Home'
import { Login } from './components/features/Login'
import { Register } from './components/features/Register'
import { Icon } from './components/ui/Icon'
import './App.css'

/**
 * Shell da aplicacao: header + navegacao por estado de pagina + footer.
 * A sessao vive no AuthProvider; a navegacao no usePage; as telas sao
 * componentes de feature — este arquivo apenas compoe.
 */
function Shell() {
  const [page, setPage] = usePage()
  const { aluno, sair } = useAuth()

  const abrirRegistro = () => setPage('register')
  const abrirLogin = () => setPage('login')
  const abrirDashboard = () => setPage(aluno ? 'dashboard' : 'login')
  // encerra a sessao e volta ao inicio; o usePage persiste a pagina nova,
  // dispensando limpeza manual do storage aqui.
  const encerrarSessao = () => {
    sair()
    setPage('home')
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <button className="brand" onClick={() => setPage('home')} aria-label="Ir para o início">
          <span className="brand-mark"><Icon name="spark" /></span>
          <span>Level<span>Up</span></span>
        </button>

        <nav className="topnav" aria-label="Navegação principal">
          <button className={page === 'home' ? 'active' : ''} onClick={() => setPage('home')}>Início</button>
          {aluno && <button className={page === 'dashboard' ? 'active' : ''} onClick={abrirDashboard}>Dashboard</button>}
        </nav>

        {aluno ? (
          <button className="avatar-button" onClick={abrirDashboard} title="Abrir dashboard">
            <span>{aluno.name.charAt(0).toUpperCase()}</span>
          </button>
        ) : (
          <div className="header-auth">
            <button className="header-login" onClick={abrirLogin}>Entrar</button>
            <button className="header-register" onClick={abrirRegistro}>Criar conta</button>
          </div>
        )}
      </header>

      {page === 'home' && (
        <Home alunoLogado={Boolean(aluno)} onRegistrar={abrirRegistro} onDashboard={abrirDashboard} />
      )}
      {page === 'register' && (
        <Register onSucesso={() => setPage('dashboard')} onTrocarParaLogin={abrirLogin} />
      )}
      {page === 'login' && (
        <Login onSucesso={() => setPage('dashboard')} onTrocarParaRegistro={abrirRegistro} />
      )}
      {page === 'dashboard' && (
        <Dashboard onRegistrar={abrirRegistro} onEntrar={abrirLogin} onSair={encerrarSessao} />
      )}

      <footer><span>LEVELUP © 2026</span><span>Aprenda. Conquiste. Evolua.</span></footer>
    </div>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <Shell />
    </AuthProvider>
  )
}
