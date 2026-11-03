package com.example.gamificacao.grupo_7.aluno.controller;

import com.example.gamificacao.grupo_7.aluno.service.AlunoService;
import com.example.gamificacao.grupo_7.aluno.dto.AlunoResponseDTO;
import com.example.gamificacao.grupo_7.aluno.dto.AlunosPaginadoResponseDTO;
import com.example.gamificacao.grupo_7.aluno.dto.AlunoRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/alunos")
@RequiredArgsConstructor
@Validated
public class AlunoController {

    private final AlunoService alunoService;

    @PostMapping
    public ResponseEntity<AlunoResponseDTO> postAluno(
                                       @RequestBody
                                       @Valid
                                       AlunoRequestDTO requestDTO
    ){
        var aluno = this.alunoService.criaAluno(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(aluno);
    }

    /**
     * Listagem paginada (evidencia de carga do Desafio 1): janela de consulta
     * sob controle do chamador, nunca a base inteira.
     */
    @GetMapping
    public ResponseEntity<AlunosPaginadoResponseDTO> getAlunos(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "pagina nao pode ser negativa")
            Integer pagina,
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "tamanho minimo e 1")
            @Max(value = 100, message = "tamanho maximo e 100")
            Integer tamanho
    ){
        var alunos = this.alunoService.getAlunos(pagina, tamanho);
        return ResponseEntity.status(HttpStatus.OK).body(alunos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlunoResponseDTO> getAlunoById(@PathVariable UUID id){
        var aluno = this.alunoService.getAlunoById(id);
        return ResponseEntity.status(HttpStatus.OK).body(aluno);
    }

    @GetMapping("/ra/{ra}")
    public ResponseEntity<AlunoResponseDTO> getAlunoByRa(@PathVariable String ra){
        var aluno = this.alunoService.getAlunoByRA(ra);
        return ResponseEntity.status(HttpStatus.OK).body(aluno);
    }

}
