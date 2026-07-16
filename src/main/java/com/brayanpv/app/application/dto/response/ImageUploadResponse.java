package com.brayanpv.app.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Outcome of a POST /bird/detect submission")
public class ImageUploadResponse implements Serializable {

    @Schema(description = "Id of the created ImageEvent")
    private UUID imageEventId;
    @Schema(description = "PROCESSING (pipeline didn't finish within the 6s timeout), NOT_A_BIRD, DONE, or FAILED", example = "DONE")
    private String status;
    @Schema(description = "Identified species id; null until classification completes")
    private UUID specieId;
    @Schema(description = "Classification confidence; null until classification completes")
    private BigDecimal speciesConfidence;
}
