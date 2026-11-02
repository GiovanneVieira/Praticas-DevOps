package com.example.gamificacao.grupo_7.voucher;

import com.example.gamificacao.grupo_7.voucher.model.VoucherValue;
import com.example.gamificacao.grupo_7.aluno.Aluno;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class VoucherMapper {

    public Voucher buildEntity(String name,
                               Double valor,
                               String description,
                               Aluno aluno
                              ){
        return Voucher.builder()
                .nome(name)
                .valor(new VoucherValue(valor))
                .descricao(description)
                .aluno(aluno)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .status(VoucherStatus.VALIDO)
                .build();
    }

    public VoucherResponseDTO toResponseDTO(Voucher voucher){
        return new VoucherResponseDTO(
                voucher.getId(),
                voucher.getNome(),
                voucher.getValor().getValorEmCentavos() / VoucherValue.CENTAVOS_POR_REAL,
                voucher.getDescricao(),
                voucher.getStatus(),
                voucher.getExpiresAt()
        );
    }

}
