import { useEffect, useState } from 'react'
import { getVouchers, type VoucherResponseDTO } from '../services/api'

/**
 * Vouchers conquistados pelo aluno (TDD6). O parametro `versao` permite ao
 * Dashboard forcar refresh apos conclusoes (o backend pode emitir voucher).
 * Fetch no effect usa then-callbacks (setState apenas em callback, nunca
 * sincrono no corpo do effect) com guarda de cancelamento.
 */
export function useVouchers(alunoId: string | undefined, versao: number) {
  const [vouchers, setVouchers] = useState<VoucherResponseDTO[]>([])
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    if (!alunoId) return
    let cancelado = false
    getVouchers(alunoId)
      .then((lista) => { if (!cancelado) setVouchers(lista) })
      .catch(() => { if (!cancelado) setVouchers([]) })
      .finally(() => { if (!cancelado) setCarregando(false) })
    return () => { cancelado = true }
  }, [alunoId, versao])

  return { vouchers, carregando }
}
