package com.example.gamificacao.grupo_7.ranking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RF01 do Desafio 1: leitura pesada servida do read model dedicado, com
 * janela controlada pelo parametro top (default 20, limites 1..100).
 */
@RestController
@RequestMapping("/ranking")
@RequiredArgsConstructor
@Validated
public class RankingController {

    private final RankingService rankingService;

    @GetMapping
    public ResponseEntity<List<RankingResponseDTO>> getRanking(
            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "top deve ser no minimo 1")
            @Max(value = 100, message = "top deve ser no maximo 100")
            Integer top
    ){
        return ResponseEntity.ok(this.rankingService.getRanking(top));
    }

}
