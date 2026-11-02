package com.example.gamificacao.grupo_7.repository;

import com.example.gamificacao.grupo_7.enums.Conquista;
import com.example.gamificacao.grupo_7.model.ConquistaConcedida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ConquistaConcedidaRepository extends JpaRepository<ConquistaConcedida, UUID> {

    List<ConquistaConcedida> findByAlunoId(UUID alunoId);

    boolean existsByAlunoIdAndConquista(UUID alunoId, Conquista conquista);

}
