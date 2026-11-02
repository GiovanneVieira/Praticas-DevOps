package com.example.gamificacao.grupo_7.matricula;

import com.example.gamificacao.grupo_7.shared.CursoStatus;
import org.springframework.stereotype.Component;


@Component
public class CursoMapper {

    public Curso buildEntity(String name, Double notaFinal){
        return Curso.builder()
                .name(name)
                .status(CursoStatus.INICIADO)
                .notaFinal(notaFinal)
                .build();
    }

}
