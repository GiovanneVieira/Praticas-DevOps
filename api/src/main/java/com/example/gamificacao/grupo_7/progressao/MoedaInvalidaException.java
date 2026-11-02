package com.example.gamificacao.grupo_7.progressao;

import com.example.gamificacao.grupo_7.shared.BusinessException;

public class MoedaInvalidaException extends BusinessException {
    public MoedaInvalidaException(String message) {
        super(message);
    }
}
