package com.example.gamificacao.grupo_7.aluno.service;

import com.example.gamificacao.grupo_7.aluno.EmailAlreadyExistsException;
import com.example.gamificacao.grupo_7.aluno.AuthenticationFailedException;
import com.example.gamificacao.grupo_7.aluno.AlunoRepository;
import com.example.gamificacao.grupo_7.aluno.AlunoNotFoundException;
import com.example.gamificacao.grupo_7.aluno.Aluno;
import com.example.gamificacao.grupo_7.aluno.model.RA;
import com.example.gamificacao.grupo_7.aluno.dto.LoginRequestDTO;
import com.example.gamificacao.grupo_7.aluno.dto.AlunoResponseDTO;
import com.example.gamificacao.grupo_7.aluno.dto.AlunoRequestDTO;
import com.example.gamificacao.grupo_7.aluno.mapper.AlunoMapper;
import com.example.gamificacao.grupo_7.shared.Plano;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class AlunoServiceTest {

    @Mock
    AlunoRepository alunoRepository;
    @Mock
    AlunoMapper alunoMapper;
    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AlunoService alunoService;

    private Aluno buildAlunoSalvo(){
        return Aluno.builder()
                .id(UUID.randomUUID())
                .name("teste")
                .email("email@teste.com")
                .password("123456")
                .ra(new RA("2026123"))
                .plano(Plano.BASICO)
                .moedas(0)
                .build();
    }

    @Test
    void deveCriarAluno(){

        log.info("Iniciando teste de criacao de aluno");

        AlunoRequestDTO request = new AlunoRequestDTO(
                "teste",
                "email@teste.com",
                "123456"
        );

        var entity = this.buildAlunoSalvo();
        var responseEsperada = new AlunoResponseDTO(entity.getId(), "teste", entity.getRa().getRa(), Plano.BASICO, entity.getMoedas(), entity.getCursosConcluidos());

        when(this.alunoRepository.existsByEmail("email@teste.com")).thenReturn(false);
        when(this.alunoMapper.toEntity(request)).thenReturn(entity);
        when(this.alunoRepository.save(entity)).thenReturn(entity);
        when(this.alunoMapper.toResponseDTO(entity)).thenReturn(responseEsperada);

        var alunoCriado = this.alunoService.criaAluno(request);

        assertEquals("teste", alunoCriado.name());
        assertEquals(Plano.BASICO, alunoCriado.plano());
        verify(this.alunoRepository).save(entity);
    }

    @Test
    void naoDeveCriarAlunoComEmailJaCadastrado(){
        AlunoRequestDTO request = new AlunoRequestDTO(
                "teste",
                "email@teste.com",
                "123456"
        );

        when(this.alunoRepository.existsByEmail("email@teste.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class, () -> this.alunoService.criaAluno(request));
        verify(this.alunoRepository, never()).save(any());
    }

    @Test
    void deveBuscarAlunoPorId(){
        var aluno = this.buildAlunoSalvo();
        var responseEsperada = new AlunoResponseDTO(aluno.getId(), "teste", aluno.getRa().getRa(), Plano.BASICO, aluno.getMoedas(), aluno.getCursosConcluidos());

        when(this.alunoRepository.findById(aluno.getId())).thenReturn(Optional.of(aluno));
        when(this.alunoMapper.toResponseDTO(aluno)).thenReturn(responseEsperada);

        var encontrado = this.alunoService.getAlunoById(aluno.getId());

        assertEquals(aluno.getId(), encontrado.id());
        assertEquals("teste", encontrado.name());
    }

    @Test
    void deveLancarExcecaoQuandoAlunoNaoExistePorId(){
        UUID idInexistente = UUID.randomUUID();

        when(this.alunoRepository.findById(idInexistente)).thenReturn(Optional.empty());

        assertThrows(AlunoNotFoundException.class, () -> this.alunoService.getAlunoById(idInexistente));
    }

    @Test
    void deveBuscarAlunoPorRA(){
        var aluno = this.buildAlunoSalvo();
        var responseEsperada = new AlunoResponseDTO(aluno.getId(), "teste", aluno.getRa().getRa(), Plano.BASICO, aluno.getMoedas(), aluno.getCursosConcluidos());

        when(this.alunoRepository.findByRa(aluno.getRa())).thenReturn(Optional.of(aluno));
        when(this.alunoMapper.toResponseDTO(aluno)).thenReturn(responseEsperada);

        var encontrado = this.alunoService.getAlunoByRA(aluno.getRa().getRa());

        assertEquals(aluno.getId(), encontrado.id());
    }

    @Test
    void deveLancarExcecaoQuandoAlunoNaoExistePorRA(){
        when(this.alunoRepository.findByRa(any(RA.class))).thenReturn(Optional.empty());

        assertThrows(AlunoNotFoundException.class, () -> this.alunoService.getAlunoByRA("2026999"));
    }

    /**
     * Listagem paginada (GREEN da correcao apontada pela evidencia de carga
     * do Desafio 1: GET /alunos sem janela tinha custo O(n) com a base):
     *
     *   Dado uma pagina de resultado do repositorio com totais
     *   Quando a listagem e consultada com pagina/tamanho
     *   Entao o DTO devolve o conteudo mapeado e os totais (7 itens, 4 paginas de 2)
     */
    @Test
    void deveListarAlunosPaginados(){
        var aluno1 = this.buildAlunoSalvo();
        var aluno2 = Aluno.builder()
                .id(UUID.randomUUID()).name("Segundo").email("b@t.com")
                .password("x").ra(new RA("2026002")).plano(Plano.BASICO)
                .moedas(0).cursosConcluidos(0).build();
        var dto1 = new AlunoResponseDTO(aluno1.getId(), "teste", aluno1.getRa().getRa(), Plano.BASICO, aluno1.getMoedas(), aluno1.getCursosConcluidos());
        var dto2 = new AlunoResponseDTO(aluno2.getId(), "Segundo", aluno2.getRa().getRa(), Plano.BASICO, aluno2.getMoedas(), aluno2.getCursosConcluidos());
        var paginaDoRepo = new org.springframework.data.domain.PageImpl<>(
                List.of(aluno1, aluno2), org.springframework.data.domain.PageRequest.of(1, 2), 7);

        when(this.alunoRepository.findAll(org.springframework.data.domain.PageRequest.of(1, 2))).thenReturn(paginaDoRepo);
        when(this.alunoMapper.toResponseDTO(aluno1)).thenReturn(dto1);
        when(this.alunoMapper.toResponseDTO(aluno2)).thenReturn(dto2);

        var pagina = this.alunoService.getAlunos(1, 2);

        assertEquals(2, pagina.conteudo().size());
        assertEquals("teste", pagina.conteudo().getFirst().name());
        assertEquals(1, pagina.pagina());
        assertEquals(2, pagina.tamanho());
        assertEquals(7, pagina.totalElementos());
        assertEquals(4, pagina.totalPaginas());
    }

    /**
     * Login (GREEN do endpoint de autenticacao):
     *
     *   Dado um aluno cadastrado com senha hasheada em BCrypt
     *   Quando envia email e senha corretos
     *   Entao recebe os dados do aluno
     */
    @Test
    void deveFazerLoginComCredenciaisValidas(){
        var aluno = this.buildAlunoSalvo();
        var responseEsperada = new AlunoResponseDTO(aluno.getId(), "teste", aluno.getRa().getRa(), Plano.BASICO, aluno.getMoedas(), aluno.getCursosConcluidos());

        when(this.alunoRepository.findByEmail("email@teste.com")).thenReturn(Optional.of(aluno));
        when(this.passwordEncoder.matches("123456", aluno.getPassword())).thenReturn(true);
        when(this.alunoMapper.toResponseDTO(aluno)).thenReturn(responseEsperada);

        var logado = this.alunoService.login(new LoginRequestDTO("email@teste.com", "123456"));

        assertEquals(aluno.getId(), logado.id());
    }

    @Test
    void naoDeveFazerLoginComSenhaIncorreta(){
        var aluno = this.buildAlunoSalvo();
        when(this.alunoRepository.findByEmail("email@teste.com")).thenReturn(Optional.of(aluno));
        when(this.passwordEncoder.matches("senha-errada", aluno.getPassword())).thenReturn(false);

        assertThrows(AuthenticationFailedException.class,
                () -> this.alunoService.login(new LoginRequestDTO("email@teste.com", "senha-errada")));
    }

    @Test
    void naoDeveFazerLoginComEmailInexistente(){
        when(this.alunoRepository.findByEmail("ninguem@teste.com")).thenReturn(Optional.empty());

        assertThrows(AuthenticationFailedException.class,
                () -> this.alunoService.login(new LoginRequestDTO("ninguem@teste.com", "123456")));
    }

}
