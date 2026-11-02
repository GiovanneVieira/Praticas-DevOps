package com.example.gamificacao.grupo_7.matricula.dto;

import com.example.gamificacao.grupo_7.shared.CursoStatus;

import java.util.UUID;

public record MatriculaResponseDTO(
        UUID id,
        String nomeCurso,
        CursoStatus status,
        Double notaFinal
) {
}
