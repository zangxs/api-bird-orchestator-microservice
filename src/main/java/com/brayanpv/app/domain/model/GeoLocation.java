package com.brayanpv.app.domain.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class GeoLocation {

    private BigDecimal latitude;
    private BigDecimal longitude;
}
