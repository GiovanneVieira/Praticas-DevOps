package com.example.gamificacao.grupo_7.aluno;

import com.example.gamificacao.grupo_7.shared.BusinessException;

public class AuthenticationFailedException extends BusinessException {
    public AuthenticationFailedException(String message) {
        super(message);
    }
}
