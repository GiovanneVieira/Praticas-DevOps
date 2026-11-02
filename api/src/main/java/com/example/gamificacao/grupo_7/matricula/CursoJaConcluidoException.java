package com.example.gamificacao.grupo_7.matricula;

import com.example.gamificacao.grupo_7.shared.BusinessException;

public class CursoJaConcluidoException extends BusinessException {
    public CursoJaConcluidoException(String message) {
        super(message);
    }
}
