package com.brayanpv.app.application.dto.response;

import com.brayanpv.app.domain.model.enums.ImageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@Schema(description = "One of a user's bird sightings")
public class BirdSightingResponse implements Serializable {
    private UUID imageEventId;
    private UUID specieId;
    private String scientificName;
    private String commonName;
    @Schema(description = "Presigned S3 URL, valid for 30 minutes")
    private String thumbnailUrl;
    private ImageStatus status;
}
