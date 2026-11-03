package com.example.gamificacao.grupo_7.aluno.service;

import com.example.gamificacao.grupo_7.aluno.EmailAlreadyExistsException;
import com.example.gamificacao.grupo_7.aluno.AuthenticationFailedException;
import com.example.gamificacao.grupo_7.aluno.AlunoRepository;
import com.example.gamificacao.grupo_7.aluno.AlunoNotFoundException;
import com.example.gamificacao.grupo_7.aluno.model.RA;
import com.example.gamificacao.grupo_7.aluno.dto.LoginRequestDTO;
import com.example.gamificacao.grupo_7.aluno.dto.AlunoResponseDTO;
import com.example.gamificacao.grupo_7.aluno.dto.AlunosPaginadoResponseDTO;
import com.example.gamificacao.grupo_7.aluno.dto.AlunoRequestDTO;
import com.example.gamificacao.grupo_7.aluno.mapper.AlunoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlunoService {

    private final AlunoRepository alunoRepository;
    private final AlunoMapper alunoMapper;
    private final PasswordEncoder passwordEncoder;

    public AlunoResponseDTO criaAluno(AlunoRequestDTO requestDTO){
        if(this.alunoRepository.existsByEmail(requestDTO.email())){
            throw new EmailAlreadyExistsException("O email " + requestDTO.email() + " ja existe");
        }
        var newAluno = alunoMapper.toEntity(requestDTO);
        var savedAluno = this.alunoRepository.save(newAluno);

        return this.alunoMapper.toResponseDTO(savedAluno);
    }

    public AlunoResponseDTO getAlunoById(UUID id){
        var aluno = this.alunoRepository.findById(id).orElseThrow(() -> new AlunoNotFoundException("Aluno com id " + id + " nao encontrado"));
        return this.alunoMapper.toResponseDTO(aluno);
    }

    public AlunoResponseDTO getAlunoByRA(String ra){
        var aluno = this.alunoRepository.findByRa(new RA(ra)).orElseThrow(() -> new AlunoNotFoundException("Aluno com RA " + ra + " nao encontrado"));
        return this.alunoMapper.toResponseDTO(aluno);
    }

    /**
     * Listagem paginada: janela sob controle do chamador (ate 100 por pagina)
     * — nunca a base inteira. Mapeia a Page do Spring Data para DTO proprio,
     * sem vazar tipos de persistencia no contrato.
     */
    public AlunosPaginadoResponseDTO getAlunos(int pagina, int tamanho){
        var resultado = this.alunoRepository.findAll(PageRequest.of(pagina, tamanho));
        return new AlunosPaginadoResponseDTO(
                resultado.getContent().stream().map(alunoMapper::toResponseDTO).toList(),
                resultado.getNumber(),
                resultado.getSize(),
                resultado.getTotalElements(),
                resultado.getTotalPages()
        );
    }

    /**
     * Login simples com BCrypt: compara a senha do input com o hash
     * armazenado via passwordEncoder.matches. Mensagem generica tanto para
     * email inexistente quanto para senha incorreta, para nao vazar qual
     * campo errou.
     */
    public AlunoResponseDTO login(LoginRequestDTO requestDTO){
        var aluno = this.alunoRepository.findByEmail(requestDTO.email())
                .orElseThrow(() -> new AuthenticationFailedException("Credenciais invalidas"));

        if (!this.passwordEncoder.matches(requestDTO.password(), aluno.getPassword())){
            throw new AuthenticationFailedException("Credenciais invalidas");
        }

        return this.alunoMapper.toResponseDTO(aluno);
    }
}
