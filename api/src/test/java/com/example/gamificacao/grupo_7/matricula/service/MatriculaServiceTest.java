package com.example.gamificacao.grupo_7.matricula.service;

import com.example.gamificacao.grupo_7.matricula.MatriculaNotFoundException;
import com.example.gamificacao.grupo_7.matricula.dto.MatriculaResponseDTO;
import com.example.gamificacao.grupo_7.matricula.dto.MatriculaRequestDTO;
import com.example.gamificacao.grupo_7.matricula.repository.MatriculaRepository;
import com.example.gamificacao.grupo_7.matricula.mapper.MatriculaMapper;
import com.example.gamificacao.grupo_7.matricula.model.Matricula;
import com.example.gamificacao.grupo_7.matricula.mapper.CursoMapper;
import com.example.gamificacao.grupo_7.matricula.model.Curso;
import com.example.gamificacao.grupo_7.shared.CursoStatus;
import com.example.gamificacao.grupo_7.shared.Plano;
import com.example.gamificacao.grupo_7.aluno.AlunoNotFoundException;
import com.example.gamificacao.grupo_7.aluno.Aluno;
import com.example.gamificacao.grupo_7.aluno.model.RA;
import com.example.gamificacao.grupo_7.aluno.AlunoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MatriculaServiceTest {

    private static final UUID ALUNO_ID = UUID.randomUUID();
    private static final UUID MATRICULA_ID = UUID.randomUUID();

    @Mock
    MatriculaRepository matriculaRepository;
    @Mock
    AlunoRepository alunoRepository;
    @Mock
    CursoMapper cursoMapper;
    @Mock
    MatriculaMapper matriculaMapper;

    @InjectMocks
    MatriculaService matriculaService;

    private Aluno buildAluno(){
        return Aluno.builder()
                .id(ALUNO_ID)
                .name("Aluno Teste")
                .email("aluno@teste.com")
                .password("123456")
                .ra(new RA("2026123"))
                .plano(Plano.BASICO)
                .moedas(0)
                .build();
    }

    @Test
    void deveMatricularAlunoEmNovoCurso(){
        var aluno = this.buildAluno();
        var curso = Curso.builder()
                .name("Curso Teste")
                .status(CursoStatus.INICIADO)
                .notaFinal(0.0)
                .build();
        var matricula = Matricula.builder()
                .id(MATRICULA_ID)
                .aluno(aluno)
                .curso(curso)
                .build();

        when(this.alunoRepository.findById(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(this.cursoMapper.buildEntity("Curso Teste", 0.0)).thenReturn(curso);
        when(this.matriculaMapper.toEntity(aluno, curso)).thenReturn(matricula);
        when(this.matriculaRepository.save(matricula)).thenReturn(matricula);
        when(this.matriculaMapper.toResponseDTO(matricula))
                .thenReturn(new MatriculaResponseDTO(MATRICULA_ID, "Curso Teste", CursoStatus.INICIADO, 0.0));

        var resposta = this.matriculaService.matricularAluno(ALUNO_ID, new MatriculaRequestDTO("Curso Teste"));

        assertEquals(MATRICULA_ID, resposta.id());
        assertEquals(CursoStatus.INICIADO, resposta.status());
    }

    @Test
    void deveListarMatriculasDoAluno(){
        var aluno = this.buildAluno();
        var matricula = Matricula.builder()
                .id(MATRICULA_ID)
                .aluno(aluno)
                .curso(Curso.builder().name("Curso Teste").status(CursoStatus.CONCLUIDO).notaFinal(9.0).build())
                .build();

        when(this.alunoRepository.findById(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(this.matriculaRepository.findAllByAlunoId(ALUNO_ID)).thenReturn(List.of(matricula));
        when(this.matriculaMapper.toResponseDTO(matricula))
                .thenReturn(new MatriculaResponseDTO(MATRICULA_ID, "Curso Teste", CursoStatus.CONCLUIDO, 9.0));

        var matriculas = this.matriculaService.listarMatriculas(ALUNO_ID);

        assertEquals(1, matriculas.size());
        assertEquals(CursoStatus.CONCLUIDO, matriculas.getFirst().status());
    }

    @Test
    void deveLancarExcecaoQuandoAlunoNaoExisteAoMatricular(){
        when(this.alunoRepository.findById(ALUNO_ID)).thenReturn(Optional.empty());

        assertThrows(AlunoNotFoundException.class,
                () -> this.matriculaService.matricularAluno(ALUNO_ID, new MatriculaRequestDTO("Curso Teste")));
        verify(this.matriculaRepository, never()).save(any(Matricula.class));
    }

    /**
     * Desistencia (GREEN do endpoint de remocao):
     *
     *   Dado um aluno com uma matricula em seu historico
     *   Quando ele desiste do curso
     *   Entao a matricula e removida do repositorio
     */
    @Test
    void deveDesistirDeMatriculaERemoverDoHistorico(){
        var aluno = this.buildAluno();
        var matricula = Matricula.builder()
                .id(MATRICULA_ID)
                .aluno(aluno)
                .curso(Curso.builder().name("Curso Teste").status(CursoStatus.INICIADO).notaFinal(0.0).build())
                .build();

        when(this.alunoRepository.findById(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(this.matriculaRepository.findByIdAndAlunoId(MATRICULA_ID, ALUNO_ID)).thenReturn(Optional.of(matricula));

        this.matriculaService.desistirMatricula(ALUNO_ID, MATRICULA_ID);

        verify(this.matriculaRepository).delete(matricula);
    }

    @Test
    void deveLancarExcecaoQuandoMatriculaNaoExisteAoDesistir(){
        var aluno = this.buildAluno();
        when(this.alunoRepository.findById(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(this.matriculaRepository.findByIdAndAlunoId(MATRICULA_ID, ALUNO_ID)).thenReturn(Optional.empty());

        assertThrows(MatriculaNotFoundException.class,
                () -> this.matriculaService.desistirMatricula(ALUNO_ID, MATRICULA_ID));
        verify(this.matriculaRepository, never()).delete(any(Matricula.class));
    }

    /**
     * TDD4 - progresso persistido (guarda de regressao da desistencia):
     *
     *   Dado um aluno com 5 cursos concluidos no progresso persistido
     *   Quando ele apaga um curso concluido do historico (desistencia)
     *   Entao a matricula e removida do repositorio
     *   E o progresso persistido permanece 5 (progresso conquistado nao se apaga)
     */
    @Test
    void naoDeveReduzirProgressoPersistidoAoApagarCursoConcluidoDoHistorico(){
        var aluno = this.buildAluno();
        aluno.setCursosConcluidos(5);
        var matricula = Matricula.builder()
                .id(MATRICULA_ID)
                .aluno(aluno)
                .curso(Curso.builder().name("Curso Concluido").status(CursoStatus.CONCLUIDO).notaFinal(9.0).build())
                .build();

        when(this.alunoRepository.findById(ALUNO_ID)).thenReturn(Optional.of(aluno));
        when(this.matriculaRepository.findByIdAndAlunoId(MATRICULA_ID, ALUNO_ID)).thenReturn(Optional.of(matricula));

        this.matriculaService.desistirMatricula(ALUNO_ID, MATRICULA_ID);

        verify(this.matriculaRepository).delete(matricula);
        assertEquals(5, aluno.getCursosConcluidos(), "Apagar curso do historico nao deve reduzir o progresso");
        verify(this.alunoRepository, never()).save(any(Aluno.class));
    }

}
