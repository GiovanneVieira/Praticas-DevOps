package com.example.gamificacao.grupo_7.ranking;

import com.example.gamificacao.grupo_7.matricula.CursoConcluidoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumidor de eventos de conclusao para o ranking (RF01 do Desafio 1).
 * AFTER_COMMIT sincrono: reage somente a progresso ja persistido e mantem o
 * comportamento deterministico para os testes de ponta a ponta.
 *
 * REQUIRES_NEW e obrigatorio: em AFTER_COMMIT o EntityManager da transacao
 * original permanece vinculado a thread ja completado - sem uma transacao
 * nova, os writes do listener ficariam sem flush e seriam silenciosamente
 * descartados (pitfall documentado do @TransactionalEventListener).
 */
@Component
@RequiredArgsConstructor
public class RankingEventListener {

    private final RankingService rankingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void aoConcluirCurso(CursoConcluidoEvent event){
        this.rankingService.atualizar(event);
    }

}
