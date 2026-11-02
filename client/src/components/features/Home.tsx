import { Icon, type IconName } from '../ui/Icon'

function Feature({ icon, number, title, text }: { icon: IconName, number: string, title: string, text: string }) {
  return <article className="feature-card"><div className="feature-icon"><Icon name={icon} /></div><span>{number}</span><h3>{title}</h3><p>{text}</p></article>
}

export function Home({ alunoLogado, onRegistrar, onDashboard }: {
  alunoLogado: boolean
  onRegistrar: () => void
  onDashboard: () => void
}) {
  return (
    <main className="home-page">
      <section className="hero-section">
        <div className="hero-copy">
          <div className="eyebrow"><span className="pulse-dot" /> Aprenda. Conquiste. Evolua.</div>
          <h1>Transforme seus<br /><em>estudos</em> em conquistas.</h1>
          <p>Uma experiência de aprendizagem gamificada para acompanhar seu progresso, desbloquear novos desafios e tornar cada curso uma nova conquista.</p>
          <div className="hero-actions">
            <button className="primary-button" onClick={onRegistrar}>Começar agora <Icon name="arrow" /></button>
            {alunoLogado && <button className="secondary-button" onClick={onDashboard}>Meu dashboard</button>}
          </div>
          <div className="trust-row">
            <div><strong>100%</strong><span>foco no aprendizado</span></div>
            <div><strong>XP</strong><span>por cada conquista</span></div>
            <div><strong>∞</strong><span>possibilidades</span></div>
          </div>
        </div>

        <div className="hero-visual" aria-hidden="true">
          <div className="glow" />
          <div className="floating-card card-top"><span className="mini-icon"><Icon name="check" /></span><div><b>Curso concluído!</b><small>+250 XP conquistados</small></div></div>
          <div className="profile-orb"><div className="orb-ring" /><span>LU</span></div>
          <div className="level-card"><span>NÍVEL ATUAL</span><strong>07</strong><div className="xp-bar"><i /></div><small>2.450 / 3.000 XP</small></div>
          <div className="floating-card card-bottom"><span className="coin-mini"><Icon name="coin" /></span><div><b>1.280 moedas</b><small>Saldo disponível</small></div></div>
        </div>
      </section>

      <section className="features-section">
        <div className="section-heading"><span>COMO FUNCIONA</span><h2>Seu progresso, do seu jeito.</h2></div>
        <div className="feature-grid">
          <Feature icon="book" number="01" title="Aprenda" text="Avance pelos seus cursos e acompanhe seu desenvolvimento." />
          <Feature icon="spark" number="02" title="Conquiste" text="Complete desafios e transforme dedicação em recompensas." />
          <Feature icon="trophy" number="03" title="Desbloqueie" text="Use suas conquistas para acessar novas possibilidades." />
        </div>
      </section>

      <section className="cta-section">
        <div><span className="eyebrow">PRONTO PARA COMEÇAR?</span><h2>Seu próximo nível<br />começa aqui.</h2></div>
        <button className="primary-button" onClick={onRegistrar}>Criar minha conta <Icon name="arrow" /></button>
      </section>
    </main>
  )
}
