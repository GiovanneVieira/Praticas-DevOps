package com.example.gamificacao.grupo_7.conquistas.service;

import com.example.gamificacao.grupo_7.matricula.CursoConcluidoEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumidor de eventos de conclusao para as conquistas (RF02 do Desafio 1).
 * Mesmo evento do ranking, consumidor independente: adicionar conquistas nao
 * alterou uma linha do fluxo de conclusao nem do ranking.
 *
 * REQUIRES_NEW pelo mesmo motivo do RankingEventListener: writes em
 * AFTER_COMMIT precisam de transacao propria para nao serem descartados.
 */
@Component
@RequiredArgsConstructor
public class ConquistaEventListener {

    private final ConquistaService conquistaService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void aoConcluirCurso(CursoConcluidoEvent event){
        this.conquistaService.avaliar(event);
    }

}
