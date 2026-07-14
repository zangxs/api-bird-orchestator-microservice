package com.brayanpv.app.application.dto.response;

import com.brayanpv.app.domain.model.enums.ImageStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class ImageStatusResponse implements Serializable {

    private UUID imageEventId;
    private ImageStatus status;
    private UUID specieId;
    private String scientificName;
    private String commonName;
    private BigDecimal specieConfidence;
    private String failureReason;
}
