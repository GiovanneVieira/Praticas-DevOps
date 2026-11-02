import type { FormEvent } from 'react'
import type { MatriculaResponseDTO } from '../../services/api'
import { Icon } from '../ui/Icon'

interface CoursesPanelProps {
  matriculas: MatriculaResponseDTO[]
  carregando: boolean
  erro: string
  enviando: boolean
  onMatricular: (event: FormEvent<HTMLFormElement>) => void
  onConcluir: (matriculaId: string, notaTexto: string) => void
  onDesistir: (matricula: MatriculaResponseDTO) => void
}

/** Painel de matriculas (AC1): formulario, lista e acoes por curso. */
export function CoursesPanel({ matriculas, carregando, erro, enviando, onMatricular, onConcluir, onDesistir }: CoursesPanelProps) {
  return (
    <div className="panel courses-panel">
      <div className="panel-head"><div><span>APRENDIZADO</span><h2>Minhas matrículas</h2></div><span className="live-label">DADOS DA API</span></div>

      <form className="matricula-form" onSubmit={onMatricular}>
        <input name="nomeCurso" type="text" placeholder="Nome do curso (ex.: DevOps Essentials)" required />
        <button className="primary-button" disabled={enviando}>{enviando ? '...' : 'Matricular'}</button>
      </form>

      {erro && <div className="error-box">{erro}</div>}
      {carregando && <p className="loading-note">Carregando matrículas...</p>}

      <div className="course-list">
        {!carregando && matriculas.length === 0 && (
          <p className="loading-note">Nenhuma matrícula ainda. Cadastre seu primeiro curso acima.</p>
        )}
        {matriculas.map(matricula => (
          <div className="course-row" key={matricula.id}>
            <div className="course-icon"><Icon name={matricula.status === 'CONCLUIDO' ? 'check' : matricula.status === 'REPROVADO' ? 'close' : 'book'} /></div>
            <div className="course-info">
              <b>{matricula.nomeCurso}</b>
              <small>{matricula.status === 'INICIADO' ? 'Curso em andamento' : `Nota final: ${matricula.notaFinal?.toFixed(1) ?? '—'}`}</small>
              <div className="progress"><i style={{ width: matricula.status === 'CONCLUIDO' ? '100%' : matricula.status === 'REPROVADO' ? '35%' : '55%' }} /></div>
            </div>
            <div className="course-actions">
              {matricula.status === 'INICIADO' && (
                <form className="concluir-form" onSubmit={(e) => { e.preventDefault(); const nota = String(new FormData(e.currentTarget).get('nota') || ''); onConcluir(matricula.id, nota) }}>
                  <input name="nota" type="number" min={0} max={10} step={0.1} placeholder="Nota" required />
                  <button type="submit" disabled={enviando}>Concluir</button>
                </form>
              )}
              <button
                type="button"
                className="desistir-button"
                disabled={enviando}
                onClick={() => onDesistir(matricula)}
              >
                {matricula.status === 'INICIADO' ? 'Desistir' : 'Apagar'}
              </button>
              <span className={`status-chip ${matricula.status.toLowerCase()}`}>{matricula.status}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  )
}
