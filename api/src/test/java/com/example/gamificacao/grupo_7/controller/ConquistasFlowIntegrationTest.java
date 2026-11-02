package com.example.gamificacao.grupo_7.controller;

import com.example.gamificacao.grupo_7.AbstractIntegrationTest;
import com.example.gamificacao.grupo_7.dto.aluno.AlunoRequestDTO;
import com.example.gamificacao.grupo_7.dto.aluno.AlunoResponseDTO;
import com.example.gamificacao.grupo_7.dto.conquista.ConquistaResponseDTO;
import com.example.gamificacao.grupo_7.dto.matricula.ConcluirCursoRequestDTO;
import com.example.gamificacao.grupo_7.dto.matricula.MatriculaRequestDTO;
import com.example.gamificacao.grupo_7.dto.matricula.MatriculaResponseDTO;
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
 * Testes de aceitacao do RF02 do Desafio 1 (Conquistas): exercitam o fluxo
 * HTTP completo contra PostgreSQL real via Testcontainers, verificando que as
 * conquistas sao desbloqueadas pelos eventos de conclusao de curso, expostas
 * com o catalogo completo ("como obter") e nunca duplicadas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ConquistasFlowIntegrationTest extends AbstractIntegrationTest {

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

    private void concluirProximoCurso(UUID alunoId, double notaFinal){
        var resposta = this.client.patch()
                .uri("/alunos/" + alunoId + "/matriculas/" + this.primeiraMatriculaIniciada(alunoId) + "/conclusao")
                .body(new ConcluirCursoRequestDTO(notaFinal))
                .retrieve()
                .toEntity(Object.class);
        assertEquals(HttpStatus.OK, resposta.getStatusCode());
    }

    private void semearMatricula(UUID alunoId, String nomeCurso){
        var resposta = this.client.post()
                .uri("/alunos/" + alunoId + "/matriculas")
                .body(new MatriculaRequestDTO(nomeCurso))
                .retrieve()
                .toEntity(MatriculaResponseDTO.class);
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
    }

    private List<ConquistaResponseDTO> conquistas(UUID alunoId){
        ConquistaResponseDTO[] resposta = this.client.get()
                .uri("/alunos/" + alunoId + "/conquistas")
                .retrieve()
                .body(ConquistaResponseDTO[].class);
        return List.of(resposta);
    }

    private void levarAteProgresso(UUID alunoId, int progressoAlvo){
        this.semearMatricula(alunoId, "Curso Semente");
        this.concluirProximoCurso(alunoId, 9.0);
        for (int progresso = 2; progresso <= progressoAlvo; progresso++){
            this.concluirProximoCurso(alunoId, 9.0);
        }
    }

    /**
     * RF02 - cenario de aceitacao (integracao de ponta a ponta):
     *
     * Dado um aluno basico que concluiu o primeiro curso com nota 9
     * Quando suas conquistas sao consultadas
     * Entao "Primeira Conclusao" esta desbloqueada com data de concessao
     * E as demais permanecem bloqueadas com progresso e descricao de como obter
     */
    @Test
    void concluirPrimeiroCursoDeveDesbloquearPrimeiraConclusaoEMostrarProgressoDasDemais(){
        var alunoId = this.criaAluno("conquista-primeira@teste.com");
        this.levarAteProgresso(alunoId, 1);

        var conquistas = this.conquistas(alunoId);

        assertEquals(6, conquistas.size(), "O catalogo completo deve ser listado");
        assertEquals(6, conquistas.stream().map(ConquistaResponseDTO::id).distinct().count());

        var primeira = this.busca(conquistas, "PRIMEIRA_CONCLUSAO");
        assertTrue(primeira.desbloqueada());
        assertNotNull(primeira.concedidaEm());
        assertNotNull(primeira.descricao(), "A listagem deve explicar como obter cada conquista");

        var maratonista = this.busca(conquistas, "MARATONISTA");
        assertFalse(maratonista.desbloqueada());
        assertNull(maratonista.concedidaEm());
        assertEquals(1, maratonista.progressoAtual());
        assertEquals(12, maratonista.progressoAlvo());

        var elite = this.busca(conquistas, "ELITE");
        assertFalse(elite.desbloqueada());
        assertNull(elite.progressoAtual());
    }

    /**
     * RF02 - cenario de aceitacao (integracao de ponta a ponta):
     *
     * Dado um aluno que atingiu o 12o curso (Premium, 3 moedas)
     * Quando suas conquistas sao consultadas
     * Entao os marcos de cursos (1, 5, 12) e o marco de plano estao desbloqueados
     * E os marcos de moedas (10, 25) seguem bloqueados com progresso 3
     * E nenhuma conquista aparece duplicada
     */
    @Test
    void atingir12oCursoDeveDesbloquearMarcosDeCursosEPlanoSemDuplicar(){
        var alunoId = this.criaAluno("conquista-maratonista@teste.com");
        this.levarAteProgresso(alunoId, 12);

        var conquistas = this.conquistas(alunoId);

        assertEquals(6, conquistas.size());
        assertTrue(this.busca(conquistas, "PRIMEIRA_CONCLUSAO").desbloqueada());
        assertTrue(this.busca(conquistas, "ESTUDANTE_DEDICADO").desbloqueada());
        assertTrue(this.busca(conquistas, "MARATONISTA").desbloqueada());
        assertTrue(this.busca(conquistas, "ELITE").desbloqueada());

        var colecionador = this.busca(conquistas, "COLECIONADOR");
        assertFalse(colecionador.desbloqueada(), "3 moedas nao atingem o marco de 10");
        assertEquals(3, colecionador.progressoAtual());
        assertEquals(10, colecionador.progressoAlvo());
        assertFalse(this.busca(conquistas, "MAGNATA").desbloqueada());

        // progresso adicional nao gera duplicatas nem novas concessoes indevidas
        this.levarAteProgresso(alunoId, 13);
        var aposProgressoExtra = this.conquistas(alunoId);
        assertEquals(6, aposProgressoExtra.size());
        assertEquals(1, aposProgressoExtra.stream()
                .filter(c -> c.id().equals("MARATONISTA") && c.desbloqueada())
                .count(), "Conquista concedida nao deve ser duplicada");
    }

    /**
     * RF02 - cenario de aceitacao: aluno inexistente recebe 404.
     */
    @Test
    void conquistasDeveRetornar404ParaAlunoInexistente(){
        var excecao = assertThrows(
                org.springframework.web.client.RestClientResponseException.class,
                () -> this.conquistas(UUID.randomUUID()));
        assertEquals(HttpStatus.NOT_FOUND, excecao.getStatusCode());
    }

    private ConquistaResponseDTO busca(List<ConquistaResponseDTO> conquistas, String id){
        return conquistas.stream()
                .filter(c -> c.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Conquista " + id + " ausente do catalogo"));
    }

}
