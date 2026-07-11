package com.brayanpv.app.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ImageUploadResponse implements Serializable {

    private UUID imageEventId;
    private String status;
    private UUID specieId; //null si aun no aplica
    private BigDecimal speciesConfidence;  // null si aún no aplica
}
