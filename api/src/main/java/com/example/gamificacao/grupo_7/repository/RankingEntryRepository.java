package com.example.gamificacao.grupo_7.repository;

import com.example.gamificacao.grupo_7.model.RankingEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RankingEntryRepository extends JpaRepository<RankingEntry, UUID> {

    Optional<RankingEntry> findByAlunoId(UUID alunoId);

    /**
     * Top-N do ranking ordenado por moedas (criterio principal), desempate
     * por cursos concluidos e desempate final alfabetico pelo nome - chaves
     * completas garantem ordem estavel entre consultas; o limite vem por
     * Pageable para manter a janela de leitura sob controle do chamador.
     */
    @Query("select r from RankingEntry r order by r.moedas desc, r.cursosConcluidos desc, r.nomeAluno asc")
    List<RankingEntry> findRanking(Pageable pageable);

}
