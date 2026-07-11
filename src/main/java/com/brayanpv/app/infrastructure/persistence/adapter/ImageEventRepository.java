package com.brayanpv.app.infrastructure.persistence.adapter;

import com.brayanpv.app.domain.exception.ImageEventNotFoundException;
import com.brayanpv.app.domain.exception.SpecieNotFoundException;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.infrastructure.mapper.ImageEventMapper;
import com.brayanpv.app.infrastructure.persistence.repository.IImageEventR2DBCRepository;
import com.brayanpv.app.infrastructure.persistence.repository.ISpecieR2DBCRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Log4j2
public class ImageEventRepository implements IImageEventRepository {

    private final IImageEventR2DBCRepository imageEventR2DBCRepository;
    private final ImageEventMapper imageEventMapper;
    private final ISpecieR2DBCRepository specieR2DBCRepository;

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
    public Mono<ImageEvent> updateClassification(UUID imageEventId, String specie, BigDecimal specieConfidence, String failureReason) {
        log.info("Updating classification result for image event {}", imageEventId);

        return imageEventR2DBCRepository.findById(imageEventId)
                .switchIfEmpty(Mono.error(new ImageEventNotFoundException("Not found: " + imageEventId)))
                .flatMap(entity -> {
                    if (specie == null) {
                        entity.setStatus(ImageStatus.FAILED);
                        entity.setFailureReason(failureReason);
                        return Mono.just(entity);
                    }

                    return specieR2DBCRepository.findByScientificName(specie)
                            .switchIfEmpty(Mono.error(new SpecieNotFoundException("Not found: " + specie)))
                            .map(specieEntity -> {
                                entity.setSpeciesId(specieEntity.getId());
                                entity.setSpeciesConfidence(specieConfidence);
                                entity.setStatus(ImageStatus.DONE);
                                return entity;
                            });
                })
                .flatMap(imageEventR2DBCRepository::save)
                .map(imageEventMapper::toModelFromEntity);
    }

    @Override
    public Flux<ImageEvent> findByStatus(ImageStatus status) {
        return imageEventR2DBCRepository.findByStatus(status)
                .map(imageEventMapper::toModelFromEntity);
    }

    @Override
    public Mono<ImageEvent> markExpired(UUID imageEventId) {
        log.info("Marking image event {} as expired", imageEventId);
        return imageEventR2DBCRepository.findById(imageEventId)
                .switchIfEmpty(Mono.error(new ImageEventNotFoundException("Not found: " + imageEventId)))
                .map(entity -> {
                    entity.setStatus(ImageStatus.EXPIRED);
                    return entity;
                })
                .flatMap(imageEventR2DBCRepository::save)
                .map(imageEventMapper::toModelFromEntity);
    }
}
