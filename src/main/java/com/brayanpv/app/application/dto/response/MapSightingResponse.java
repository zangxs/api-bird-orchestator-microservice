package com.brayanpv.app.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class MapSightingResponse implements Serializable {

    private String scientificName;
    private String commonName;
    private String thumbnailUrl;
    private UUID imageEventId;
    private UUID speciesId;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
