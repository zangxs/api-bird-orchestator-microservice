package com.brayanpv.app.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
public class GenericResponse<T> implements Serializable {
    private T data;
    private int code;
    private LocalDateTime dateTime;
}
