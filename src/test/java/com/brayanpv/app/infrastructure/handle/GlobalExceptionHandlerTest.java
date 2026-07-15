package com.brayanpv.app.infrastructure.handle;

import com.brayanpv.app.application.dto.response.GenericResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleException_wrapsMessageInBadRequestEnvelope() {
        RuntimeException ex = new RuntimeException("boom");

        ResponseEntity<GenericResponse<Object>> response = handler.handleException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        GenericResponse<Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(body.getData()).isEqualTo("boom");
        assertThat(body.getDateTime()).isNotNull();
    }
}
