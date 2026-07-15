package com.brayanpv.app.domain.usecase.contracts;

import com.brayanpv.app.application.dto.response.MapSightingResponse;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.UUID;

public interface IBirdMapInformationUseCase {
    Flux<MapSightingResponse> execute(BigDecimal minLat, BigDecimal maxLat, BigDecimal minLng, BigDecimal maxLng);

}
