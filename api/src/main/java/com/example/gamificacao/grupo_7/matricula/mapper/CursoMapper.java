package com.example.gamificacao.grupo_7.matricula.mapper;

import com.example.gamificacao.grupo_7.matricula.model.Curso;
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
