package com.brayanpv.app.domain.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class BirdDetectionResult implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String imageEventId;
    private boolean idBird;
    private BigDecimal confidence;

}
