package com.brayanpv.app.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Envelope wrapping every API response")
public class GenericResponse<T> implements Serializable {
    @Schema(description = "Endpoint-specific payload")
    private T data;
    @Schema(description = "HTTP status code, mirrored from the response status", example = "200")
    private int code;
    @Schema(description = "Response timestamp, UTC")
    private LocalDateTime dateTime;
}
