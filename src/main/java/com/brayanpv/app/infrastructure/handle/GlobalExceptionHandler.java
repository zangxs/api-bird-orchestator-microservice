package com.brayanpv.app.infrastructure.handle;

import com.brayanpv.app.application.dto.response.GenericResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

@ControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    @ExceptionHandler(value = ConstraintViolationException.class)
    public ResponseEntity<GenericResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("Input validation failed: {}", message);

        GenericResponse<Object> genericResponse = GenericResponse.builder()
                .dateTime(LocalDateTime.now(ZoneOffset.UTC))
                .code(HttpStatus.BAD_REQUEST.value())
                .data(message)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(genericResponse);
    }

    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<GenericResponse<Object>> handleException(RuntimeException ex) {
        log.error(ex.getMessage(), ex);

        GenericResponse<Object> genericResponse = GenericResponse.builder()
                .dateTime(LocalDateTime.now(ZoneOffset.UTC))
                .code(HttpStatus.BAD_REQUEST.value())
                .data(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(genericResponse);

    }
}
