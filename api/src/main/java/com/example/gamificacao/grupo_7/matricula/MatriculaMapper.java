package com.example.gamificacao.grupo_7.matricula;

import com.example.gamificacao.grupo_7.aluno.Aluno;
import org.springframework.stereotype.Component;

@Component
public class MatriculaMapper {

    public Matricula toEntity(Aluno aluno, Curso curso){
        return Matricula.builder()
                .aluno(aluno)
                .curso(curso)
                .build();
    }

    public MatriculaResponseDTO toResponseDTO(Matricula matricula){
        var curso = matricula.getCurso();
        return new MatriculaResponseDTO(
                matricula.getId(),
                curso.getName(),
                curso.getStatus(),
                curso.getNotaFinal()
        );
    }

}
