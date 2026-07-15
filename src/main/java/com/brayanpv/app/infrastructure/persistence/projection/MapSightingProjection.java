package com.brayanpv.app.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface MapSightingProjection {
    UUID getId();
    UUID getSpeciesId();
    String getS3Key();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
}
