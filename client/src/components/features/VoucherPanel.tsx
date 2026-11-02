import type { VoucherResponseDTO } from '../../services/api'
import { Icon } from '../ui/Icon'

/** Painel de vouchers (TDD6): recompensas persistidas do aluno. */
export function VoucherPanel({ vouchers }: { vouchers: VoucherResponseDTO[] }) {
  return (
    <section className="panel voucher-panel">
      <div className="panel-head"><div><span>RECOMPENSAS</span><h2>Seus vouchers</h2></div><span className="live-label">DADOS DA API</span></div>
      {vouchers.length === 0 ? (
        <p className="loading-note"><Icon name="ticket" /> Nenhum voucher ainda. Conclua o 12º curso com nota superior a 7,0 para virar Premium e receber vouchers para projetos reais.</p>
      ) : (
        <div className="voucher-grid">{vouchers.map(v => (
          <div className="voucher" key={v.id}>
            <span className="voucher-icon"><Icon name="ticket" /></span>
            <div><b>{v.nome}</b><small>R$ {v.valorEmReais.toFixed(2)} · {v.status}{v.expiresAt ? ` · expira em ${new Date(v.expiresAt).toLocaleDateString('pt-BR')}` : ''}</small></div>
          </div>
        ))}</div>
      )}
    </section>
  )
}
