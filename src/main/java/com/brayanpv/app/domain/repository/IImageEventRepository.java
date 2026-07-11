package com.brayanpv.app.domain.repository;

import com.brayanpv.app.domain.model.ImageEvent;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

public interface IImageEventRepository {
    Mono<ImageEvent> save(ImageEvent imageEvent);
    Mono<ImageEvent> updateDetection(UUID imageEventId, boolean isBird, BigDecimal confidence);
    Mono<ImageEvent> updateClassification(UUID imageEventId, UUID specieID, BigDecimal specieConfidence, String failureReason);
}
