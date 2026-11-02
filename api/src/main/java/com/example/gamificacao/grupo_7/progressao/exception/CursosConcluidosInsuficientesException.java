package com.example.gamificacao.grupo_7.progressao.exception;

import com.example.gamificacao.grupo_7.shared.BusinessException;

public class CursosConcluidosInsuficientesException extends BusinessException {
    public CursosConcluidosInsuficientesException(String message) {
        super(message);
    }
}
