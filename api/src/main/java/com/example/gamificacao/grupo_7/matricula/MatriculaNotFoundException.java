package com.example.gamificacao.grupo_7.matricula;

import com.example.gamificacao.grupo_7.shared.BusinessException;

public class MatriculaNotFoundException extends BusinessException {
    public MatriculaNotFoundException(String message) {
        super(message);
    }
}
