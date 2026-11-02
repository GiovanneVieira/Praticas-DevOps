package com.example.gamificacao.grupo_7.progressao;

import com.example.gamificacao.grupo_7.shared.BusinessException;

public class NotaFinalNaoSuficienteException extends BusinessException {
    public NotaFinalNaoSuficienteException(String message) {
        super(message);
    }
}
