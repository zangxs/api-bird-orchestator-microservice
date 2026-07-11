package com.brayanpv.app.domain.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class BirdClassificationResult implements Serializable {

    private UUID imageEventId;
    private String specie;
    private BigDecimal specieConfidence;
    private String failureReason;
}
