package com.example.gamificacao.grupo_7.aluno;

import com.example.gamificacao.grupo_7.shared.BusinessException;

public class AlunoNotFoundException extends BusinessException {
    public AlunoNotFoundException(String s) {
        super(s);
    }
}
