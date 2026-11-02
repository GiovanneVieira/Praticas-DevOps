package com.example.gamificacao.grupo_7.ranking;

import java.util.UUID;

/**
 * Contrato de resposta do ranking (RF01 do Desafio 1): posicao calculada no
 * servico a partir da ordenacao por moedas (desempate por cursos concluidos).
 */
public record RankingResponseDTO(
        Integer posicao,
        UUID alunoId,
        String nome,
        Integer moedas,
        Integer cursosConcluidos
) {}
