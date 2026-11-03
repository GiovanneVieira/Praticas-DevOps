package com.example.gamificacao.grupo_7;

import com.example.gamificacao.grupo_7.aluno.dto.AlunoRequestDTO;
import com.example.gamificacao.grupo_7.aluno.dto.AlunoResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Correcao do RA null (integracao de ponta a ponta):
 *
 *   Dado um novo aluno cadastrado via POST /alunos
 *   Entao a resposta deve conter um RA gerado automaticamente (6 digitos)
 *   E o aluno deve ser encontravel por GET /alunos/ra/{ra}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class AlunoFlowIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    private RestClient client;

    @BeforeEach
    void setUp(){
        this.client = RestClient.builder()
                .baseUrl("http://localhost:" + this.port)
                .build();
    }

    @Test
    void alunoCriadoDeveReceberRAEPoderSerBuscadoPorRA(){
        var resposta = this.client.post()
                .uri("/alunos")
                .body(new AlunoRequestDTO("Aluno RA Teste", "aluno.ra@teste.com", "123456"))
                .retrieve()
                .toEntity(AlunoResponseDTO.class);

        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        var alunoCriado = resposta.getBody();
        assertNotNull(alunoCriado.ra(), "RA deve vir preenchido na criacao");
        assertTrue(alunoCriado.ra().matches("\\d{6}"),
                "RA deve ter 6 digitos: ano da matricula (2, ex. 26) + sorteio (4)");

        var encontrado = this.client.get()
                .uri("/alunos/ra/" + alunoCriado.ra())
                .retrieve()
                .body(AlunoResponseDTO.class);

        assertEquals(alunoCriado.id(), encontrado.id());
    }

    /**
     * Paginacao de GET /alunos (correcao da evidencia de carga do Desafio 1 —
     * resposta nao delimitada tinha custo O(n) com a base):
     *
     *   Dado 3 alunos cadastrados
     *   Quando a listagem e consultada com pagina=0&tamanho=2
     *   Entao retorna 2 itens com totalElementos=3 e totalPaginas=2
     *   E a pagina 1 traz o restante
     *   E parametros invalidos (pagina<0, tamanho>100) recebem 400
     */
    @Test
    void listarAlunosDeveVirPaginadoComValidacaoDeJanela(){
        for (int i = 1; i <= 3; i++){
            this.client.post()
                    .uri("/alunos")
                    .body(new AlunoRequestDTO("Aluno Pag " + i, "aluno.pag" + i + System.nanoTime() + "@teste.com", "123456"))
                    .retrieve()
                    .toEntity(AlunoResponseDTO.class);
        }

        var pagina1 = this.client.get()
                .uri("/alunos?pagina=0&tamanho=2")
                .retrieve().body(PaginaAlunos.class);

        assertNotNull(pagina1);
        assertEquals(2, pagina1.conteudo().size(), "janela deve respeitar o tamanho solicitado");
        assertEquals(0, pagina1.pagina());
        assertEquals(2, pagina1.tamanho());
        assertTrue(pagina1.totalElementos() >= 3, "base compartilhada entre testes: totais sao relativos");
        assertTrue(pagina1.totalPaginas() >= 2);

        var pagina2 = this.client.get()
                .uri("/alunos?pagina=1&tamanho=2")
                .retrieve().body(PaginaAlunos.class);
        assertTrue(pagina2.conteudo().size() >= 1, "segunda pagina deve trazer o restante");
        assertEquals(1, pagina2.pagina());

        for (var uriInvalida : List.of("/alunos?pagina=-1", "/alunos?tamanho=0", "/alunos?tamanho=101")){
            var excecao = assertThrows(
                    org.springframework.web.client.RestClientResponseException.class,
                    () -> this.client.get().uri(uriInvalida).retrieve().toEntity(String.class));
            assertEquals(HttpStatus.BAD_REQUEST, excecao.getStatusCode(), uriInvalida + " deve rejeitar com 400");
        }
    }

    record PaginaAlunos(List<AlunoResponseDTO> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {}

}
