package com.brayanpv.app.domain.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class MapSighting implements Serializable {

    private UUID imageEventId;
    private UUID speciesId;
    private String s3Key;
    private String commonName;
    private String scientificName;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
