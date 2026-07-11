package com.brayanpv.app.infrastructure.persistence.adapter;

import com.brayanpv.app.domain.exception.ImageEventNotFoundException;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.infrastructure.mapper.ImageEventMapper;
import com.brayanpv.app.infrastructure.persistence.repository.IImageEventR2DBCRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Log4j2
public class ImageEventRepository implements IImageEventRepository {

    private final IImageEventR2DBCRepository imageEventR2DBCRepository;
    private final ImageEventMapper imageEventMapper;

    @Override
    public Mono<ImageEvent> save(ImageEvent imageEvent) {
        log.info("Saving image event {}", imageEvent);
        return Mono.just(imageEvent)
                .flatMap(image -> Mono.just(imageEventMapper.toEntityFromModel(image)))
                .flatMap(imageEventR2DBCRepository::save)
                .flatMap(saved -> Mono.just(imageEventMapper.toModelFromEntity(saved)));

    }

    @Override
    public Mono<ImageEvent> updateDetection(UUID imageEventId, boolean isBird, BigDecimal confidence) {
        log.info("Updating detection result for image event {}", imageEventId);
        ImageStatus newStatus = isBird ? ImageStatus.BIRD_DETECTED : ImageStatus.NOT_A_BIRD;

        return imageEventR2DBCRepository.findById(imageEventId)
                .switchIfEmpty(Mono.error(new ImageEventNotFoundException("Not found: {}" + imageEventId)))
                .map(entity -> {
                    entity.setStatus(newStatus);
                    entity.setBirdConfidence(confidence);
                    return entity;
                })
                .flatMap(imageEventR2DBCRepository::save)
                .map(imageEventMapper::toModelFromEntity);
    }

    @Override
    public Mono<ImageEvent> updateClassification(UUID imageEventId, UUID specieID, BigDecimal specieConfidence, String failureReason) {
        log.info("Updating classification result for image event {}", imageEventId);

        return null;
    }
}
