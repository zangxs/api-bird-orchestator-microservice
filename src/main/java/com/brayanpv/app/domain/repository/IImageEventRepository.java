package com.brayanpv.app.domain.repository;

import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.MapSighting;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

public interface IImageEventRepository {
    Mono<ImageEvent> save(ImageEvent imageEvent);
    Mono<ImageEvent> updateDetection(UUID imageEventId, boolean isBird, BigDecimal confidence);
    Mono<ImageEvent> updateClassification(UUID imageEventId, String scientificName, BigDecimal specieConfidence, String failureReason);
    Flux<ImageEvent> findByStatus(ImageStatus status);
    Mono<ImageEvent> markExpired(UUID imageEventId);
    Mono<ImageEvent> findById(UUID imageEventId);
    Flux<ImageEvent> findByUserId(UUID userId);
    Flux<MapSighting> findDoneSightingsInBounds(BigDecimal minLat, BigDecimal maxLat, BigDecimal minLng, BigDecimal maxLng);
}
