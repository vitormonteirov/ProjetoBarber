package dev.vitor.barbearia.ProjetoBarber.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos contêm valores inválidos.");
        problem.setTitle("Erro de Validação");
        problem.setType(URI.create("https://barber.dev/errors/validation-error"));
        problem.setProperty("timestamp", Instant.now());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        problem.setProperty("errors", errors);
        problem.setProperty("message", "Erro de validação nos campos informados.");
        return problem;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso Não Encontrado");
        problem.setType(URI.create("https://barber.dev/errors/resource-not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("message", ex.getMessage());
        return problem;
    }

    @ExceptionHandler({BusinessRuleException.class, IllegalArgumentException.class})
    public ProblemDetail handleBusinessRuleException(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Violação de Regra de Negócio");
        problem.setType(URI.create("https://barber.dev/errors/business-rule-violation"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("message", ex.getMessage());
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno inesperado no servidor.");
        problem.setTitle("Erro Interno no Servidor");
        problem.setType(URI.create("https://barber.dev/errors/internal-server-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("message", "Ocorreu um erro interno inesperado no servidor.");
        return problem;
    }
}
