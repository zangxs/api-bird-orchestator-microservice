package com.brayanpv.app.application.dto.response;

import com.brayanpv.app.domain.model.enums.ImageStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Current state of an ImageEvent")
public class ImageStatusResponse implements Serializable {

    private UUID imageEventId;
    private ImageStatus status;
    @Schema(description = "Identified species id; null until classification completes")
    private UUID specieId;
    private String scientificName;
    private String commonName;
    @Schema(description = "Classification confidence; null until classification completes")
    private BigDecimal specieConfidence;
    @Schema(description = "Set when status is FAILED")
    private String failureReason;
}
