package com.example.gamificacao.grupo_7.matricula;

import com.example.gamificacao.grupo_7.shared.Plano;

import java.util.UUID;

/**
 * Evento de dominio publicado ao concluir um curso com nota aprovatoria,
 * carregando um snapshot do estado do aluno no momento da conclusao
 * (event-carried state transfer): consumidores como ranking e conquistas
 * nao precisam consultar a base de alunos para reagir, reduzindo o
 * acoplamento entre quem publica e quem escuta.
 *
 * Publicado dentro da transacao de conclusao; consumidores ouvem em
 * AFTER_COMMIT, garantindo que reagem apenas a progresso persistido.
 */
public record CursoConcluidoEvent(
        UUID alunoId,
        String nomeAluno,
        int moedas,
        int cursosConcluidos,
        Plano plano
) {}
