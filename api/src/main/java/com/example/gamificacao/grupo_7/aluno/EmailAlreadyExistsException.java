package com.example.gamificacao.grupo_7.aluno;

import com.example.gamificacao.grupo_7.shared.BusinessException;

public class EmailAlreadyExistsException extends BusinessException {
    public EmailAlreadyExistsException(String s) {
        super(s);
    }
}
