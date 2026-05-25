package com.learning.ai.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Intercepts unhandled exceptions from controllers and converts them into
 * structured JSON error responses instead of letting upstream HTTP status codes
 * (e.g. 404 from OpenAI) propagate back to the browser.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean / Jakarta validation failures (e.g. @NotBlank on ChatRequest). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validation failure at {}: {}", request.getRequestURI(), details);
        return error(HttpStatus.BAD_REQUEST, "Validation error: " + details, request);
    }

    /** @Validated constraint violations (path variables, etc.). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(
            ConstraintViolationException ex, HttpServletRequest request) {

        log.warn("Constraint violation at {}: {}", request.getRequestURI(), ex.getMessage());
        return error(HttpStatus.BAD_REQUEST, "Validation error: " + ex.getMessage(), request);
    }

    /**
     * Catch-all handler for any runtime exception thrown by the AI layer or
     * any other service.  Returning 500 with a descriptive message prevents
     * Spring Boot from forwarding an upstream HTTP status code (e.g. 404 from
     * OpenAI "model not found") directly to the browser.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(
            Exception ex, HttpServletRequest request) {

        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            msg = ex.getClass().getSimpleName() + " - check server logs for details.";
        }

        log.error("Unhandled exception at {}: {}", request.getRequestURI(), msg, ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, msg, request);
    }

    // -----------------------------------------------------------------------

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status, String message, HttpServletRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        body.put("path", request.getRequestURI());
        body.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.status(status).body(body);
    }
}

