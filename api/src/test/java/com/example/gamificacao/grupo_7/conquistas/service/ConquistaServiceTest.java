package com.example.gamificacao.grupo_7.conquistas.service;

import com.example.gamificacao.grupo_7.conquistas.repository.ConquistaConcedidaRepository;
import com.example.gamificacao.grupo_7.conquistas.model.ConquistaConcedida;
import com.example.gamificacao.grupo_7.conquistas.model.Conquista;
import com.example.gamificacao.grupo_7.shared.Plano;
import com.example.gamificacao.grupo_7.matricula.CursoConcluidoEvent;
import com.example.gamificacao.grupo_7.aluno.AlunoNotFoundException;
import com.example.gamificacao.grupo_7.aluno.Aluno;
import com.example.gamificacao.grupo_7.aluno.model.RA;
import com.example.gamificacao.grupo_7.aluno.AlunoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios do servico de conquistas (RF02 do Desafio 1): o catalogo e
 * versionado em codigo (enum Conquista) e as concessoes sao idempotentes,
 * disparadas pelo mesmo evento de conclusao de curso que alimenta o ranking.
 */
@ExtendWith(MockitoExtension.class)
public class ConquistaServiceTest {

    @Mock
    ConquistaConcedidaRepository conquistaConcedidaRepository;

    @Mock
    AlunoRepository alunoRepository;

    @InjectMocks
    ConquistaService conquistaService;

    private Aluno buildAluno(int cursosConcluidos, int moedas, Plano plano){
        return Aluno.builder()
                .id(UUID.randomUUID())
                .name("Aluno Teste")
                .email("aluno@teste.com")
                .password("123456")
                .ra(new RA("2026123"))
                .plano(plano)
                .moedas(moedas)
                .cursosConcluidos(cursosConcluidos)
                .build();
    }

    private CursoConcluidoEvent evento(UUID alunoId, int cursosConcluidos, int moedas, Plano plano){
        return new CursoConcluidoEvent(alunoId, "Aluno Teste", moedas, cursosConcluidos, plano);
    }

    /**
     * Dado um aluno que concluiu o primeiro curso (0 moedas, plano basico)
     * Quando as conquistas sao avaliadas
     * Entao apenas "Primeira Conclusao" e concedida
     */
    @Test
    void avaliarDeveConcederSomenteAsConquistasAtingidas(){
        var alunoId = UUID.randomUUID();
        when(this.conquistaConcedidaRepository.existsByAlunoIdAndConquista(alunoId, Conquista.PRIMEIRA_CONCLUSAO))
                .thenReturn(false);

        this.conquistaService.avaliar(this.evento(alunoId, 1, 0, Plano.BASICO));

        var captura = ArgumentCaptor.forClass(ConquistaConcedida.class);
        verify(this.conquistaConcedidaRepository, times(1)).save(captura.capture());
        assertEquals(Conquista.PRIMEIRA_CONCLUSAO, captura.getValue().getConquista());
        assertEquals(alunoId, captura.getValue().getAlunoId());
    }

    /**
     * Dado um aluno que atingiu todos os marcos (12 cursos, 25 moedas, Premium)
     * Quando as conquistas sao avaliadas
     * Entao todas as 6 conquistas do catalogo sao concedidas
     */
    @Test
    void avaliarDeveConcederTodasQuandoTodosOsMarcosForemAtingidos(){
        var alunoId = UUID.randomUUID();

        this.conquistaService.avaliar(this.evento(alunoId, 12, 25, Plano.PREMIUM));

        var captura = ArgumentCaptor.forClass(ConquistaConcedida.class);
        verify(this.conquistaConcedidaRepository, times(6)).save(captura.capture());
        var concedidas = captura.getAllValues().stream().map(ConquistaConcedida::getConquista).toList();
        assertEquals(6, concedidas.size());
        assertEquals(6, concedidas.stream().distinct().count(), "Nenhuma conquista duplicada");
        assertEquals(Conquista.values().length, concedidas.size());
    }

    /**
     * Dado um aluno que ja possui todas as conquistas concedidas
     * Quando um novo evento e processado
     * Entao nenhuma concessao e repetida (idempotencia)
     */
    @Test
    void avaliarDeveSerIdempotenteParaConquistasJaConcedidas(){
        var alunoId = UUID.randomUUID();
        when(this.conquistaConcedidaRepository.existsByAlunoIdAndConquista(eq(alunoId), any(Conquista.class)))
                .thenReturn(true);

        this.conquistaService.avaliar(this.evento(alunoId, 12, 25, Plano.PREMIUM));

        verify(this.conquistaConcedidaRepository, never()).save(any());
    }

    /**
     * Dado um aluno com 5 cursos concluidos que ja recebeu "Estudante Dedicado"
     * Quando a listagem de conquistas e consultada
     * Entao o catalogo completo (6) retorna com status desbloqueada + data
     *   e as bloqueadas mostram o progresso atual/alvo (ex.: 5/12)
     */
    @Test
    void listaConquistasDeveMarcarDesbloqueadasComDataEBloqueadasComProgresso(){
        var aluno = this.buildAluno(5, 0, Plano.BASICO);
        var concedidaEm = LocalDateTime.of(2026, 11, 1, 10, 0);
        when(this.alunoRepository.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        when(this.conquistaConcedidaRepository.findByAlunoId(aluno.getId())).thenReturn(List.of(
                ConquistaConcedida.builder()
                        .id(UUID.randomUUID())
                        .alunoId(aluno.getId())
                        .conquista(Conquista.ESTUDANTE_DEDICADO)
                        .concedidaEm(concedidaEm)
                        .build()
        ));

        var conquistas = this.conquistaService.listaConquistas(aluno.getId());

        assertEquals(6, conquistas.size());
        var dedicado = conquistas.stream()
                .filter(c -> c.id().equals(Conquista.ESTUDANTE_DEDICADO.name()))
                .findFirst().orElseThrow();
        assertTrue(dedicado.desbloqueada());
        assertEquals(concedidaEm, dedicado.concedidaEm());
        assertEquals(5, dedicado.progressoAtual());
        assertEquals(5, dedicado.progressoAlvo());

        var maratonista = conquistas.stream()
                .filter(c -> c.id().equals(Conquista.MARATONISTA.name()))
                .findFirst().orElseThrow();
        assertFalse(maratonista.desbloqueada());
        assertNull(maratonista.concedidaEm());
        assertEquals(5, maratonista.progressoAtual());
        assertEquals(12, maratonista.progressoAlvo());

        var elite = conquistas.stream()
                .filter(c -> c.id().equals(Conquista.ELITE.name()))
                .findFirst().orElseThrow();
        assertFalse(elite.desbloqueada());
        assertNull(elite.progressoAtual(), "Conquista de plano nao tem progresso numerico");
        assertNull(elite.progressoAlvo());
    }

    /**
     * Dado um aluno inexistente
     * Quando a listagem de conquistas e consultada
     * Entao retorna 404 (AlunoNotFoundException)
     */
    @Test
    void listaConquistasDeveLancarNotFoundParaAlunoInexistente(){
        var alunoId = UUID.randomUUID();
        when(this.alunoRepository.findById(alunoId)).thenReturn(Optional.empty());

        assertThrows(AlunoNotFoundException.class, () -> this.conquistaService.listaConquistas(alunoId));
        verify(this.conquistaConcedidaRepository, never()).findByAlunoId(any());
    }

}
