package com.example.gamificacao.grupo_7.shared;

public abstract class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
