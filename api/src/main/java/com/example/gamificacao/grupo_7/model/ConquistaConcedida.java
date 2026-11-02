package com.example.gamificacao.grupo_7.model;

import com.example.gamificacao.grupo_7.enums.Conquista;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Estado persistido das conquistas concedidas (RF02 do Desafio 1): uma linha
 * por (aluno, conquista). A restricao unica garante idempotencia mesmo com
 * reentrega de eventos - conceder duas vezes a mesma conquista e impossivel
 * no nivel do dado.
 */
@Entity
@Table(name = "conquistas_concedidas",
        uniqueConstraints = @UniqueConstraint(name = "uk_conquista_aluno",
                columnNames = {"aluno_id", "conquista"}))
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ConquistaConcedida {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "conquista", nullable = false)
    private Conquista conquista;

    @CreationTimestamp
    @Column(name = "concedida_em", updatable = false)
    private LocalDateTime concedidaEm;

}
