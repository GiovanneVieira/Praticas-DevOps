package com.example.gamificacao.grupo_7.controller;

import com.example.gamificacao.grupo_7.dto.conquista.ConquistaResponseDTO;
import com.example.gamificacao.grupo_7.service.ConquistaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * RF02 do Desafio 1: catalogo completo de conquistas com o estado do aluno,
 * aninhado sob /alunos no mesmo padrao REST de matriculas e vouchers.
 */
@RestController
@RequiredArgsConstructor
public class ConquistaController {

    private final ConquistaService conquistaService;

    @GetMapping("/alunos/{alunoId}/conquistas")
    public ResponseEntity<List<ConquistaResponseDTO>> getConquistas(@PathVariable UUID alunoId){
        return ResponseEntity.ok(this.conquistaService.listaConquistas(alunoId));
    }

}
