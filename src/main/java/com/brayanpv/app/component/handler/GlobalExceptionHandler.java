package com.brayanpv.app.component.handler;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import com.brayanspv.auth.model.response.generic.ApiResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ServerWebInputException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@ControllerAdvice
@Log4j2
public class GlobalExceptionHandler {

    @ExceptionHandler(value = ServerWebInputException.class)
    public ResponseEntity<?> handleServerWebInputException(ServerWebInputException e) {
        log.error("error in handler handleServerWebInputException: {}", e.getMessage());

        List<String> errors = extractDefaultMessages(e.getMessage());

        Map<String, Object> errorData = Map.of("errors", errors);

        ApiResponse apiResponse = ApiResponse.builder()
                .dateTime(System.currentTimeMillis())
                .code(HttpStatus.BAD_REQUEST.value())
                .data(errorData)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
    }

    private List<String> extractDefaultMessages(String message) {
        List<String> messages = new ArrayList<>();

        // Busca todos los "default message [...]" y toma el último de cada bloque de error
        Pattern pattern = Pattern.compile("default message \\[([^\\]]+)\\]");
        Matcher matcher = pattern.matcher(message);

        String lastMessage = null;
        int lastFieldErrorStart = 0;

        // Divide por cada "[Field error" para procesar error por error
        String[] fieldErrors = message.split("\\[Field error");
        for (String fieldError : fieldErrors) {
            if (fieldError.isBlank()) continue;

            Matcher m = pattern.matcher(fieldError);
            String last = null;
            while (m.find()) {
                last = m.group(1); // el último "default message" del bloque es el mensaje legible
            }
            if (last != null) {
                messages.add(last);
            }
        }

        return messages;
    }
}