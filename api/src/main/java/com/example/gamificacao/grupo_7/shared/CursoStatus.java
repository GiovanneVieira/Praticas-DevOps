package com.example.gamificacao.grupo_7.shared;

import lombok.Getter;

@Getter
public enum CursoStatus {
    INICIADO("iniciado"),
    REPROVADO("reprovado"),
    CONCLUIDO("concluído");

    private final String status;
    CursoStatus(String status){
        this.status = status;
    }

}
