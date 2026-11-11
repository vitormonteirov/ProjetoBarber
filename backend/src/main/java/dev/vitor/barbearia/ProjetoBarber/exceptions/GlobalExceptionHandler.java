package dev.vitor.barbearia.ProjetoBarber.exceptions;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Slf4j
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

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(AuthenticationException ex) {
        String msg = "Credenciais inválidas: verifique seu e-mail e senha.";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, msg);
        problem.setTitle("Falha na Autenticação");
        problem.setType(URI.create("https://barber.dev/errors/authentication-failed"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("message", msg);
        return problem;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        String msg = (ex.getMessage() != null && !ex.getMessage().equals("Access Denied"))
                ? ex.getMessage()
                : "Acesso negado: você não possui permissão para realizar esta operação.";
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, msg);
        problem.setTitle("Acesso Negado");
        problem.setType(URI.create("https://barber.dev/errors/access-denied"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("message", msg);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("[INTERNAL SERVER ERROR] Exceção não tratada em {} {}: {}",
                request != null ? request.getMethod() : "UNKNOWN",
                request != null ? request.getRequestURI() : "UNKNOWN",
                ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Ocorreu um erro interno inesperado no servidor.");
        problem.setTitle("Erro Interno no Servidor");
        problem.setType(URI.create("https://barber.dev/errors/internal-server-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("message", "Ocorreu um erro interno inesperado no servidor.");
        String correlationId = org.slf4j.MDC.get(dev.vitor.barbearia.ProjetoBarber.config.CorrelationIdFilter.MDC_CORRELATION_ID_KEY);
        if (correlationId != null) {
            problem.setProperty("correlationId", correlationId);
        }
        return problem;
    }
}

