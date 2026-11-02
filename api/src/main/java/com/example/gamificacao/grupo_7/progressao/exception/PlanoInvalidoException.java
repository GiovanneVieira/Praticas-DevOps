package com.example.gamificacao.grupo_7.progressao.exception;

import com.example.gamificacao.grupo_7.shared.BusinessException;

public class PlanoInvalidoException extends BusinessException {
    public PlanoInvalidoException(String message) {
        super(message);
    }
}
