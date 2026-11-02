package com.example.gamificacao.grupo_7.exception;

import com.example.gamificacao.grupo_7.exception.aluno.AlunoNotFoundException;
import com.example.gamificacao.grupo_7.exception.aluno.EmailAlreadyExistsException;
import com.example.gamificacao.grupo_7.exception.login.AuthenticationFailedException;
import com.example.gamificacao.grupo_7.exception.matricula.MatriculaNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(
            String message,
            HttpStatus status,
            String path,
            LocalDateTime timestamp
    ) {}

    @ExceptionHandler(AlunoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAlunoNotFound(AlunoNotFoundException ex, HttpServletRequest request){
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(MatriculaNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMatriculaNotFound(MatriculaNotFoundException ex, HttpServletRequest request){
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(EmailAlreadyExistsException ex, HttpServletRequest request){
        return buildErrorResponse(ex, HttpStatus.CONFLICT, request);
    }

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailed(AuthenticationFailedException ex, HttpServletRequest request){
        return buildErrorResponse(ex, HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request){
        return buildErrorResponse(ex, HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request){
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        ErrorResponse response = new ErrorResponse(message, HttpStatus.BAD_REQUEST, request.getRequestURI(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    /**
     * Validacao de parametros de query/path (ex.: top do ranking) - tanto via
     * Bean Validation classica quanto via validacao de metodo embutida do
     * Spring MVC, ambas respondidas como 400 com mensagem explicativa.
     */
    @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
    public ResponseEntity<ErrorResponse> handleParamValidation(Exception ex, HttpServletRequest request){
        String message;
        if (ex instanceof ConstraintViolationException violationEx){
            message = violationEx.getConstraintViolations().stream()
                    .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                    .collect(Collectors.joining("; "));
        } else {
            message = "Parametros invalidos: " + ex.getMessage();
        }

        ErrorResponse response = new ErrorResponse(message, HttpStatus.BAD_REQUEST, request.getRequestURI(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(BusinessException ex, HttpStatus status, HttpServletRequest request){
        ErrorResponse response = new ErrorResponse(ex.getMessage(), status, request.getRequestURI(), LocalDateTime.now());
        return ResponseEntity.status(status).body(response);
    }

}
