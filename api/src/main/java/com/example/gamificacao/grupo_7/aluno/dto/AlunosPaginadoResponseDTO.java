package com.example.gamificacao.grupo_7.aluno.dto;

import java.util.List;

/**
 * Contrato paginado de GET /alunos (correcao da evidencia de carga do
 * Desafio 1): a resposta delimita a janela consultada e expoe os totais —
 * custo O(tamanho) em vez de O(base).
 */
public record AlunosPaginadoResponseDTO(
        List<AlunoResponseDTO> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {}
