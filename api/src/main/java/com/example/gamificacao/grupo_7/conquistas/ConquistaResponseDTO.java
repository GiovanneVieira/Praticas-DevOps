package com.example.gamificacao.grupo_7.conquistas;

import java.time.LocalDateTime;

/**
 * Contrato de resposta das conquistas (RF02 do Desafio 1): lista sempre o
 * catalogo completo com o estado do aluno - desbloqueadas com data de
 * concessao, bloqueadas com progresso atual/alvo e a descricao de como obter.
 * Conquistas de plano nao tem progresso numerico (campos null).
 */
public record ConquistaResponseDTO(
        String id,
        String titulo,
        String descricao,
        boolean desbloqueada,
        LocalDateTime concedidaEm,
        Integer progressoAtual,
        Integer progressoAlvo
) {}
