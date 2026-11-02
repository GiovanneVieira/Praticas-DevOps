package com.example.gamificacao.grupo_7.progressao;

import com.example.gamificacao.grupo_7.shared.BusinessException;

public class CursosConcluidosInsuficientesException extends BusinessException {
    public CursosConcluidosInsuficientesException(String message) {
        super(message);
    }
}
