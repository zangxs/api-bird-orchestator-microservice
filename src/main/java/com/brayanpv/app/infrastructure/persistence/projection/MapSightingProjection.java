package com.brayanpv.app.infrastructure.persistence.projection;

import java.math.BigDecimal;
import java.util.UUID;

public interface MapSightingProjection {
    UUID getImageEventId();
    UUID getSpeciesId();
    String getS3Key();
    String getCommonName();
    String getScientificName();
    BigDecimal getLatitude();
    BigDecimal getLongitude();
}
