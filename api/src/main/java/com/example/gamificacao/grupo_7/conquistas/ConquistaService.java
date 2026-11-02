package com.example.gamificacao.grupo_7.conquistas;

import com.example.gamificacao.grupo_7.matricula.CursoConcluidoEvent;
import com.example.gamificacao.grupo_7.aluno.AlunoNotFoundException;
import com.example.gamificacao.grupo_7.aluno.Aluno;
import com.example.gamificacao.grupo_7.aluno.AlunoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Servico de conquistas (RF02 do Desafio 1): avalia marcos contra o snapshot
 * do evento de conclusao (concessao idempotente) e lista o catalogo completo
 * com o estado do aluno - desbloqueadas com data, bloqueadas com progresso.
 */
@Service
@RequiredArgsConstructor
public class ConquistaService {

    private final ConquistaConcedidaRepository conquistaConcedidaRepository;
    private final AlunoRepository alunoRepository;

    @Transactional(readOnly = true)
    public List<ConquistaResponseDTO> listaConquistas(UUID alunoId){
        var aluno = this.alunoRepository.findById(alunoId)
                .orElseThrow(() -> new AlunoNotFoundException("Aluno com id " + alunoId + " nao encontrado"));

        Map<Conquista, ConquistaConcedida> concedidas = this.conquistaConcedidaRepository.findByAlunoId(alunoId)
                .stream()
                .collect(Collectors.toMap(ConquistaConcedida::getConquista, Function.identity()));

        return Arrays.stream(Conquista.values())
                .map(conquista -> this.toResponseDTO(conquista, aluno, concedidas.get(conquista)))
                .toList();
    }

    /**
     * Concede cada conquista cujo marco foi atingido e que ainda nao foi
     * concedida ao aluno; a restricao unica no dado e a segunda barreira de
     * idempotencia (alem do exists) para reentregas concorrentes.
     */
    @Transactional
    public void avaliar(CursoConcluidoEvent event){
        for (var conquista : Conquista.values()){
            if (!conquista.atingidaPor(event.cursosConcluidos(), event.moedas(), event.plano())){
                continue;
            }
            if (this.conquistaConcedidaRepository.existsByAlunoIdAndConquista(event.alunoId(), conquista)){
                continue;
            }
            this.conquistaConcedidaRepository.save(ConquistaConcedida.builder()
                    .alunoId(event.alunoId())
                    .conquista(conquista)
                    .build());
        }
    }

    private ConquistaResponseDTO toResponseDTO(Conquista conquista, Aluno aluno, ConquistaConcedida concedida){
        var desbloqueada = concedida != null;
        var progressoAtual = conquista.progressoDe(aluno.getCursosConcluidos(), this.moedasSeguras(aluno));

        return new ConquistaResponseDTO(
                conquista.name(),
                conquista.getTitulo(),
                conquista.getDescricao(),
                desbloqueada,
                desbloqueada ? concedida.getConcedidaEm() : null,
                progressoAtual,
                progressoAtual == null ? null : conquista.getAlvo()
        );
    }

    private int moedasSeguras(Aluno aluno){
        return aluno.getMoedas() == null ? 0 : aluno.getMoedas();
    }

}
