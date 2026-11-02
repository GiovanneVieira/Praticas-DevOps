package com.example.gamificacao.grupo_7.matricula.mapper;

import com.example.gamificacao.grupo_7.matricula.dto.MatriculaResponseDTO;
import com.example.gamificacao.grupo_7.matricula.model.Matricula;
import com.example.gamificacao.grupo_7.matricula.model.Curso;
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
