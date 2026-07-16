package com.brayanpv.app.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@Schema(description = "A DONE sighting within a GET /bird/map bounding box")
public class MapSightingResponse implements Serializable {

    private String scientificName;
    private String commonName;
    @Schema(description = "Presigned S3 URL, valid for 30 minutes")
    private String thumbnailUrl;
    private UUID imageEventId;
    private UUID speciesId;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
