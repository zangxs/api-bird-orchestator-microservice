package com.brayanpv.app.model.response.generic;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse implements Serializable {

    private LocalDateTime dateTime;
    private int code;
    private Object data;

}