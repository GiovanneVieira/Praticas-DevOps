package com.example.gamificacao.grupo_7.controller;

import com.example.gamificacao.grupo_7.AbstractIntegrationTest;
import com.example.gamificacao.grupo_7.dto.aluno.AlunoRequestDTO;
import com.example.gamificacao.grupo_7.dto.aluno.AlunoResponseDTO;
import com.example.gamificacao.grupo_7.dto.matricula.ConcluirCursoRequestDTO;
import com.example.gamificacao.grupo_7.dto.matricula.MatriculaRequestDTO;
import com.example.gamificacao.grupo_7.dto.matricula.MatriculaResponseDTO;
import com.example.gamificacao.grupo_7.dto.ranking.RankingResponseDTO;
import com.example.gamificacao.grupo_7.enums.CursoStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de aceitacao do RF01 do Desafio 1 (Ranking): exercitam o fluxo HTTP
 * completo contra PostgreSQL real via Testcontainers, verificando que o read
 * model do ranking e alimentado pelos eventos de conclusao de curso e servido
 * ordenado por moedas com desempate por cursos concluidos.
 *
 * As assercoes de posicao sao relativas entre os alunos do cenario: o banco e
 * compartilhado entre as classes de teste, portanto outros alunos podem
 * coexistir no ranking.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RankingFlowIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private RestClient client;

    @BeforeEach
    void setUp(){
        this.client = RestClient.builder()
                .baseUrl("http://localhost:" + this.port)
                .build();
    }

    private UUID criaAluno(String email){
        var resposta = this.client.post()
                .uri("/alunos")
                .body(new AlunoRequestDTO("Aluno " + email, email, "123456"))
                .retrieve()
                .toEntity(AlunoResponseDTO.class);
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        return resposta.getBody().id();
    }

    private UUID matricula(UUID alunoId, String nomeCurso){
        var resposta = this.client.post()
                .uri("/alunos/" + alunoId + "/matriculas")
                .body(new MatriculaRequestDTO(nomeCurso))
                .retrieve()
                .toEntity(MatriculaResponseDTO.class);
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        return resposta.getBody().id();
    }

    private void conclui(UUID alunoId, UUID matriculaId, double notaFinal){
        var resposta = this.client.patch()
                .uri("/alunos/" + alunoId + "/matriculas/" + matriculaId + "/conclusao")
                .body(new ConcluirCursoRequestDTO(notaFinal))
                .retrieve()
                .toEntity(Object.class);
        assertEquals(HttpStatus.OK, resposta.getStatusCode());
    }

    private List<RankingResponseDTO> ranking(String query){
        RankingResponseDTO[] resposta = this.client.get()
                .uri("/ranking" + query)
                .retrieve()
                .body(RankingResponseDTO[].class);
        return List.of(resposta);
    }

    private UUID primeiraMatriculaIniciada(UUID alunoId){
        MatriculaResponseDTO[] matriculas = this.client.get()
                .uri("/alunos/" + alunoId + "/matriculas")
                .retrieve()
                .body(MatriculaResponseDTO[].class);
        return List.of(matriculas).stream()
                .filter(m -> m.status() == CursoStatus.INICIADO)
                .findFirst()
                .orElseThrow()
                .id();
    }

    /**
     * RF01 - cenario de aceitacao (integracao de ponta a ponta):
     *
     * Dado um aluno Premium (12 cursos concluidos, 3 moedas)
     * E dois alunos com 0 moedas e cursos concluidos distintos (2 e 1)
     * Quando o ranking e consultado
     * Entao o Premium lidera por moedas
     * E o desempate entre os demais ocorre por cursos concluidos
     * E cada entrada expoe nome, moedas e cursos concluidos
     */
    @Test
    void rankingDeveOrdenarPorMoedasEDesempatarPorCursosConcluidos(){
        var premium = this.criaAluno("ranking-premium@teste.com");
        this.levarAteProgresso(premium, 12);

        var doisCursos = this.criaAluno("ranking-dois@teste.com");
        this.levarAteProgresso(doisCursos, 2);

        var umCurso = this.criaAluno("ranking-um@teste.com");
        this.levarAteProgresso(umCurso, 1);

        var ranking = this.ranking("?top=50");

        var posicoes = ranking.stream().map(RankingResponseDTO::alunoId).toList();
        assertTrue(posicoes.indexOf(premium) >= 0, "Aluno Premium deve figurar no ranking");
        assertTrue(posicoes.indexOf(premium) < posicoes.indexOf(doisCursos),
                "Aluno com 3 moedas deve vir acima dos alunos com 0");
        assertTrue(posicoes.indexOf(doisCursos) < posicoes.indexOf(umCurso),
                "Desempate por cursos concluidos: 2 cursos acima de 1");

        var entradaPremium = ranking.stream()
                .filter(e -> e.alunoId().equals(premium))
                .findFirst().orElseThrow();
        assertEquals("Aluno ranking-premium@teste.com", entradaPremium.nome());
        assertEquals(3, entradaPremium.moedas());
        assertEquals(12, entradaPremium.cursosConcluidos());
        assertTrue(entradaPremium.posicao() >= 1);
    }

    /**
     * RF01 - cenario de aceitacao: o parametro top limita o tamanho do
     * ranking (leitura pesada precisa de janela controlada).
     *
     * Dado dois alunos do cenario no ranking (alem de quaisquer outros)
     * Quando o ranking e consultado com top=2 e com top=100
     * Entao top=2 devolve exatamente 2 entradas
     * E essas entradas sao o prefixo do ranking completo
     */
    @Test
    void rankingDeveLimitarATamanhoSolicitado(){
        var alunoA = this.criaAluno("ranking-limite-a@teste.com");
        var alunoB = this.criaAluno("ranking-limite-b@teste.com");
        this.levarAteProgresso(alunoA, 1);
        this.levarAteProgresso(alunoB, 1);

        var completo = this.ranking("?top=100");
        var limitado = this.ranking("?top=2");

        assertTrue(completo.size() >= 2, "O cenario garante ao menos 2 alunos ranqueados");
        assertEquals(2, limitado.size(), "top=2 deve retornar exatamente 2 entradas");
        assertEquals(
                completo.subList(0, 2).stream().map(RankingResponseDTO::alunoId).toList(),
                limitado.stream().map(RankingResponseDTO::alunoId).toList(),
                "top=2 deve devolver o prefixo do ranking completo");
    }

    /**
     * RF01 - cenario de aceitacao: validacao do parametro top.
     *
     * Dado um valor de top invalido (0, negativo ou acima de 100)
     * Quando o ranking e consultado
     * Entao a API responde 400
     */
    @Test
    void rankingDeveRejeitarTopInvalidoCom400(){
        for (var topInvalido : List.of("0", "-1", "101")){
            var excecao = assertThrows(
                    org.springframework.web.client.RestClientResponseException.class,
                    () -> this.ranking("?top=" + topInvalido));
            assertEquals(HttpStatus.BAD_REQUEST, excecao.getStatusCode(),
                    "top=" + topInvalido + " deve ser rejeitado com 400");
        }
    }

    private void levarAteProgresso(UUID alunoId, int progressoAlvo){
        this.matricula(alunoId, "Curso Semente");
        this.conclui(alunoId, this.primeiraMatriculaIniciada(alunoId), 9.0);
        for (int progresso = 2; progresso <= progressoAlvo; progresso++){
            this.conclui(alunoId, this.primeiraMatriculaIniciada(alunoId), 9.0);
        }
    }

}
