package com.example.gamificacao.grupo_7.service;

import com.example.gamificacao.grupo_7.dto.ranking.RankingResponseDTO;
import com.example.gamificacao.grupo_7.event.CursoConcluidoEvent;
import com.example.gamificacao.grupo_7.model.RankingEntry;
import com.example.gamificacao.grupo_7.repository.RankingEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servico do read model de ranking (RF01 do Desafio 1): leituras servidas
 * exclusivamente da tabela dedicada e atualizacoes vindas de eventos de
 * conclusao de curso (upsert idempotente por aluno).
 */
@Service
@RequiredArgsConstructor
public class RankingService {

    private final RankingEntryRepository rankingEntryRepository;

    @Transactional(readOnly = true)
    public List<RankingResponseDTO> getRanking(int top){
        Pageable janela = PageRequest.of(0, top);
        var entradas = this.rankingEntryRepository.findRanking(janela);

        var posicao = new int[]{0};
        return entradas.stream()
                .map(entry -> this.toResponseDTO(entry, ++posicao[0]))
                .toList();
    }

    /**
     * Upsert alimentado pelo snapshot do evento: cria a entrada do aluno no
     * primeiro evento ou atualiza a existente - nunca duplica.
     */
    @Transactional
    public void atualizar(CursoConcluidoEvent event){
        var entry = this.rankingEntryRepository.findByAlunoId(event.alunoId())
                .orElseGet(() -> RankingEntry.builder().alunoId(event.alunoId()).build());

        entry.setNomeAluno(event.nomeAluno());
        entry.setMoedas(event.moedas());
        entry.setCursosConcluidos(event.cursosConcluidos());

        this.rankingEntryRepository.save(entry);
    }

    private RankingResponseDTO toResponseDTO(RankingEntry entry, int posicao){
        return new RankingResponseDTO(
                posicao,
                entry.getAlunoId(),
                entry.getNomeAluno(),
                entry.getMoedas(),
                entry.getCursosConcluidos()
        );
    }

}
