package com.brayanpv.app.application.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.codec.multipart.FilePart;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Multipart bird-photo submission")
public record ImageUploadRequest(
        // Spring's @RequestPart(required = true) already guarantees a non-null FilePart before the
        // controller body runs, so no @NotNull here - would only add friction to unit tests that
        // pass null to exercise other fields.
        FilePart image,
        @NotNull UUID userId,
        @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude,
        @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude
) {
}
