package com.example.gamificacao.grupo_7.conquistas.model;


import com.example.gamificacao.grupo_7.shared.Plano;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de dominio do catalogo de conquistas (RF02 do Desafio 1): os
 * criterios de desbloqueio sao regras de negocio versionadas em codigo e
 * avaliadas por limite fechado (atingir exatamente o marco desbloqueia).
 */
public class ConquistaTest {

    /**
     * Dado um aluno com exatamente 5 cursos concluidos
     * Entao "Estudante Dedicado" (alvo 5) esta desbloqueada
     *   e "Maratonista" (alvo 12) nao
     */
    @Test
    void conquistasDeCursosDevemRespeitarOMarcoExato(){
        assertTrue(Conquista.ESTUDANTE_DEDICADO.atingidaPor(5, 0, Plano.BASICO));
        assertFalse(Conquista.ESTUDANTE_DEDICADO.atingidaPor(4, 0, Plano.BASICO));
        assertFalse(Conquista.MARATONISTA.atingidaPor(11, 0, Plano.BASICO));
        assertTrue(Conquista.MARATONISTA.atingidaPor(12, 0, Plano.BASICO));
    }

    /**
     * Dado um aluno com exatamente 10 moedas acumuladas
     * Entao "Colecionador" (alvo 10) esta desbloqueada
     *   e "Magnata" (alvo 25) nao
     */
    @Test
    void conquistasDeMoedasDevemRespeitarOMarcoExato(){
        assertTrue(Conquista.COLECIONADOR.atingidaPor(0, 10, Plano.BASICO));
        assertFalse(Conquista.COLECIONADOR.atingidaPor(0, 9, Plano.BASICO));
        assertFalse(Conquista.MAGNATA.atingidaPor(0, 24, Plano.PREMIUM));
        assertTrue(Conquista.MAGNATA.atingidaPor(0, 25, Plano.PREMIUM));
    }

    /**
     * Dado um aluno com muitos cursos e moedas mas no plano basico
     * Entao "Elite" continua bloqueada: so o plano Premium desbloqueia
     */
    @Test
    void conquistaEliteDeveExigirPlanoPremium(){
        assertTrue(Conquista.ELITE.atingidaPor(0, 0, Plano.PREMIUM));
        assertFalse(Conquista.ELITE.atingidaPor(50, 100, Plano.BASICO));
    }

    /**
     * Dado o progresso atual de um aluno
     * Entao o progresso de conquistas por cursos/moedas e limitado ao alvo
     *   e conquistas de plano nao tem progresso numerico (null)
     */
    @Test
    void progressoDeveSerLimitadoAoAlvoENuloParaConquistasDePlano(){
        assertEquals(7, Conquista.MARATONISTA.progressoDe(7, 0));
        assertEquals(12, Conquista.MARATONISTA.progressoDe(30, 0));
        assertEquals(4, Conquista.MAGNATA.progressoDe(0, 4));
        assertEquals(25, Conquista.MAGNATA.progressoDe(0, 99));
        assertNull(Conquista.ELITE.progressoDe(30, 99));
    }

}
