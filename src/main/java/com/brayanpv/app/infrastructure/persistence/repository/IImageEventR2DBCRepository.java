package com.brayanpv.app.infrastructure.persistence.repository;

import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.infrastructure.persistence.entity.ImageEventEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface IImageEventR2DBCRepository extends ReactiveCrudRepository<ImageEventEntity, UUID> {
    Flux<ImageEventEntity> findByStatus(ImageStatus status);
    Flux<ImageEventEntity> findByUserIdAndStatus(UUID userId,  ImageStatus status);
}
