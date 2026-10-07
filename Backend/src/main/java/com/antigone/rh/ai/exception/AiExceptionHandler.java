package com.antigone.rh.ai.exception;

import com.antigone.rh.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Traduction des erreurs des endpoints IA.
 *
 * <p>Restreint au package {@code ai.controller} et prioritaire sur
 * {@code GlobalExceptionHandler} : sans cela son {@code @ExceptionHandler(RuntimeException)}
 * transformerait tous les refus d'acces en 400, ce qui masquerait un 403.
 *
 * <p>Le corps suit le format {@code {"error": "...", "code": "..."}} contractualise
 * dans API_DOCUMENTATION.md, distinct de l'enveloppe {@code ApiResponse} du reste de
 * l'application.
 */
@RestControllerAdvice(basePackages = "com.antigone.rh.ai.controller")
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AiExceptionHandler {

    @Data
    @AllArgsConstructor
    public static class AiErrorResponse {
        private String error;
        private String code;
    }

    @ExceptionHandler(AiForbiddenException.class)
    public ResponseEntity<AiErrorResponse> handleForbidden(AiForbiddenException ex) {
        log.warn("Acces IA refuse : {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new AiErrorResponse(ex.getMessage(), "FORBIDDEN"));
    }

    @ExceptionHandler(AiUnavailableException.class)
    public ResponseEntity<AiErrorResponse> handleUnavailable(AiUnavailableException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new AiErrorResponse(ex.getMessage(), "AI_UNAVAILABLE"));
    }

    @ExceptionHandler(AiRateLimitException.class)
    public ResponseEntity<AiErrorResponse> handleRateLimit(AiRateLimitException ex) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new AiErrorResponse(ex.getMessage(), "RATE_LIMITED"));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<AiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new AiErrorResponse(ex.getMessage(), "NOT_FOUND"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new AiErrorResponse(details.isBlank() ? "Requete invalide" : details, "VALIDATION_ERROR"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<AiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AiErrorResponse(ex.getMessage(), "BAD_REQUEST"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<AiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Erreur inattendue sur un endpoint IA : {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AiErrorResponse("Erreur interne de l'assistant.", "INTERNAL_ERROR"));
    }
}
