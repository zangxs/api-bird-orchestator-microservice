package com.brayanpv.app.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BirdDetectionResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private UUID imageEventId;
    @JsonProperty("isBird")
    private boolean isBird;
    private BigDecimal confidence;

}
