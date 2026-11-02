package com.example.gamificacao.grupo_7.ranking;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read model do ranking (RF01 do Desafio 1): visao desnormalizada e dedicada
 * a leitura pesada, alimentada por eventos de conclusao de curso. O endpoint
 * de ranking nunca consulta a tabela de alunos - o crescimento desproporcional
 * dessa funcionalidade nao compete com as escritas do fluxo academico.
 *
 * O indice composto espelha a ordenacao servida (moedas DESC, cursos
 * concluidos DESC) para que o top-N seja resolvido pelo indice.
 */
@Entity
@Table(name = "ranking_entries",
        uniqueConstraints = @UniqueConstraint(name = "uk_ranking_aluno", columnNames = "aluno_id"),
        indexes = @Index(name = "idx_ranking_ordenacao", columnList = "moedas DESC, cursos_concluidos DESC"))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class RankingEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(name = "nome_aluno", nullable = false)
    private String nomeAluno;

    @Column(name = "moedas")
    private Integer moedas;

    @Column(name = "cursos_concluidos")
    private Integer cursosConcluidos;

    @UpdateTimestamp
    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

}
