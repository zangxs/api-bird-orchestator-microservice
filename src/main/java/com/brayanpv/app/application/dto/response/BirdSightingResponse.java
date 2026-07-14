package com.brayanpv.app.application.dto.response;

import com.brayanpv.app.domain.model.enums.ImageStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
public class BirdSightingResponse implements Serializable {
    private UUID imageEventId;
    private UUID speciesId;
    private String scientificName;
    private String commonName;
    private String thumbnailUrl; // presigned URL
    private ImageStatus status;
}
