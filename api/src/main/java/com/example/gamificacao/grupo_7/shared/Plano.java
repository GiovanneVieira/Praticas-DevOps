package com.example.gamificacao.grupo_7.shared;

import lombok.Getter;

@Getter
public enum Plano {
    PREMIUM("premium"),
    BASICO("plano");

    private String plano;

    Plano(String plano){
        this.plano = plano;
    }

}
