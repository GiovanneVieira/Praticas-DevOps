package com.example.gamificacao.grupo_7.ranking;

import com.example.gamificacao.grupo_7.matricula.CursoConcluidoEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes unitarios do servico de ranking (RF01 do Desafio 1): o ranking e um
 * read model alimentado por eventos de conclusao de curso, ordenado por moedas
 * com desempate por cursos concluidos.
 */
@ExtendWith(MockitoExtension.class)
public class RankingServiceTest {

    @Mock
    RankingEntryRepository rankingEntryRepository;

    @InjectMocks
    RankingService rankingService;

    private RankingEntry buildEntry(UUID alunoId, String nome, int moedas, int cursosConcluidos){
        return RankingEntry.builder()
                .id(UUID.randomUUID())
                .alunoId(alunoId)
                .nomeAluno(nome)
                .moedas(moedas)
                .cursosConcluidos(cursosConcluidos)
                .build();
    }

    private CursoConcluidoEvent evento(UUID alunoId, int moedas, int cursosConcluidos){
        return new CursoConcluidoEvent(alunoId, "Aluno " + alunoId, moedas, cursosConcluidos, null);
    }

    /**
     * Dado um ranking com tres entradas ordenadas pelo repositorio
     * Quando o ranking e consultado
     * Entao cada DTO recebe a posicao correta (1, 2, 3) com os dados da entrada
     */
    @Test
    void getRankingDeveAtribuirPosicoesSequenciaisAEntradasOrdenadas(){
        var primeiro = this.buildEntry(UUID.randomUUID(), "Top", 3, 12);
        var segundo = this.buildEntry(UUID.randomUUID(), "Empatado A", 0, 2);
        var terceiro = this.buildEntry(UUID.randomUUID(), "Empatado B", 0, 1);
        when(this.rankingEntryRepository.findRanking(any(Pageable.class)))
                .thenReturn(List.of(primeiro, segundo, terceiro));

        var ranking = this.rankingService.getRanking(10);

        assertEquals(3, ranking.size());
        assertEquals(1, ranking.get(0).posicao());
        assertEquals(primeiro.getAlunoId(), ranking.get(0).alunoId());
        assertEquals("Top", ranking.get(0).nome());
        assertEquals(3, ranking.get(0).moedas());
        assertEquals(12, ranking.get(0).cursosConcluidos());
        assertEquals(2, ranking.get(1).posicao());
        assertEquals("Empatado A", ranking.get(1).nome());
        assertEquals(3, ranking.get(2).posicao());
        assertEquals("Empatado B", ranking.get(2).nome());
    }

    /**
     * Dado um tamanho solicitado de ranking
     * Quando o ranking e consultado
     * Entao o repositorio recebe uma pagina com exatamente esse limite
     */
    @Test
    void getRankingDeveRespeitarOTamanhoSolicitado(){
        when(this.rankingEntryRepository.findRanking(any(Pageable.class))).thenReturn(List.of());

        this.rankingService.getRanking(5);

        verify(this.rankingEntryRepository).findRanking(PageRequest.of(0, 5));
    }

    /**
     * Dado um evento de conclusao de um aluno sem entrada no ranking
     * Quando o evento e processado
     * Entao uma nova entrada e criada com os dados do evento
     */
    @Test
    void atualizarDeveCriarEntradaNovaParaAlunoAindaNaoRanqueado(){
        var alunoId = UUID.randomUUID();
        when(this.rankingEntryRepository.findByAlunoId(alunoId)).thenReturn(Optional.empty());
        when(this.rankingEntryRepository.save(any(RankingEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        this.rankingService.atualizar(this.evento(alunoId, 3, 12));

        var captura = ArgumentCaptor.forClass(RankingEntry.class);
        verify(this.rankingEntryRepository).save(captura.capture());
        var entry = captura.getValue();
        assertEquals(alunoId, entry.getAlunoId());
        assertEquals("Aluno " + alunoId, entry.getNomeAluno());
        assertEquals(3, entry.getMoedas());
        assertEquals(12, entry.getCursosConcluidos());
    }

    /**
     * Dado um aluno que ja possui entrada no ranking com valores antigos
     * Quando um novo evento do mesmo aluno e processado
     * Entao a entrada existente e atualizada em vez de duplicada
     */
    @Test
    void atualizarDeveAtualizarEntradaExistenteSemDuplicar(){
        var alunoId = UUID.randomUUID();
        var existente = this.buildEntry(alunoId, "Nome Antigo", 0, 1);
        when(this.rankingEntryRepository.findByAlunoId(alunoId)).thenReturn(Optional.of(existente));
        when(this.rankingEntryRepository.save(any(RankingEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        this.rankingService.atualizar(this.evento(alunoId, 3, 12));

        var captura = ArgumentCaptor.forClass(RankingEntry.class);
        verify(this.rankingEntryRepository).save(captura.capture());
        var entry = captura.getValue();
        assertSame(existente, entry, "A mesma instancia deve ser atualizada, nao substituida");
        assertEquals(existente.getId(), entry.getId());
        assertEquals("Aluno " + alunoId, entry.getNomeAluno());
        assertEquals(3, entry.getMoedas());
        assertEquals(12, entry.getCursosConcluidos());
    }

}
