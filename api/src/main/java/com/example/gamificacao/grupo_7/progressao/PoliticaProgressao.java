package com.example.gamificacao.grupo_7.progressao;

import com.example.gamificacao.grupo_7.shared.CursoStatus;
import com.example.gamificacao.grupo_7.shared.Plano;

public interface PoliticaProgressao {

/**
 * Contrato da regra de decisao do fluxo ATDD: dado o contexto da conclusao de um
 * curso, decide qual cenario da planilha Template_ATDD_Gamificacao.xlsx (aba pb)
 * se aplica e qual recompensa deve ser concedida.
 *
 * Mantida como abstracao (DIP) para que novos cenarios de progressao entrem como
 * novas implementacoes, sem alterar o orquestrador ConclusaoCursoService (OCP).
 *
 * @param statusAposConclusao   status do curso apos aplicar a nota (CONCLUIDO ou REPROVADO)
 * @param planoAluno            plano atual do aluno
 * @param cursosConcluidosAntes quantidade de cursos CONCLUIDOS do aluno antes desta conclusao
 */
    ResultadoProgressao avaliar(CursoStatus statusAposConclusao, Plano planoAluno, long cursosConcluidosAntes);

}
