package com.brayanpv.app.infrastructure.persistence.adapter;

import com.brayanpv.app.domain.exception.ImageEventNotFoundException;
import com.brayanpv.app.domain.exception.SpecieNotFoundException;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.MapSighting;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.infrastructure.mapper.ImageEventMapper;
import com.brayanpv.app.infrastructure.persistence.cache.SpecieCacheService;
import com.brayanpv.app.infrastructure.persistence.entity.SpecieEntity;
import com.brayanpv.app.infrastructure.persistence.repository.IImageEventR2DBCRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Log4j2
public class ImageEventRepository implements IImageEventRepository {

    private final IImageEventR2DBCRepository imageEventR2DBCRepository;
    private final ImageEventMapper imageEventMapper;
    private final SpecieCacheService specieCacheService;

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
    public Mono<ImageEvent> updateClassification(UUID imageEventId, String scientificName, BigDecimal specieConfidence, String failureReason) {
        log.info("Updating classification result for image event {}", imageEventId);

        return imageEventR2DBCRepository.findById(imageEventId)
                .switchIfEmpty(Mono.error(new ImageEventNotFoundException("Not found: " + imageEventId)))
                .flatMap(entity -> {
                    if (scientificName == null) {
                        entity.setStatus(ImageStatus.FAILED);
                        entity.setFailureReason(failureReason);
                        return Mono.just(entity);
                    }
                    return specieCacheService.findByScientificName(scientificName)
                            .switchIfEmpty(Mono.error(new SpecieNotFoundException("Not found: " + scientificName)))
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

    @Override
    public Mono<ImageEvent> findById(UUID imageEventId) {
        log.info("Finding image event by Id{}", imageEventId);
        return imageEventR2DBCRepository.findById(imageEventId)
                .switchIfEmpty(Mono.error(new ImageEventNotFoundException("Not found: " + imageEventId)))
                .map(imageEventMapper::toModelFromEntity)
                .flatMap(imageEvent -> {
                    if (imageEvent.getSpecieId() == null) {
                        return Mono.just(imageEvent);
                    }
                    return specieCacheService.findById(imageEvent.getSpecieId())
                            .switchIfEmpty(Mono.error(new SpecieNotFoundException("Not found By UUID: " + imageEvent.getSpecieId())))
                            .map(especieEntity -> {
                                imageEvent.setScientificName(especieEntity.getScientificName());
                                return imageEvent;
                            });
                })
                .doOnNext(imageEvent -> log.info("Image event {}", imageEvent));
    }

    @Override
    public Flux<ImageEvent> findByUserId(UUID userId) {
        log.info("Finding image event by user id {}", userId);
        return imageEventR2DBCRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, ImageStatus.DONE)
                .map(imageEventMapper::toModelFromEntity)
                .collectList()
                .flatMapMany(imageEvents -> {
                    List<UUID> specieIds = imageEvents.stream().map(ImageEvent::getSpecieId).toList();
                    return specieCacheService.findAllByIds(specieIds)
                            .flatMapMany(speciesById -> Flux.fromIterable(imageEvents)
                                    .map(imageEvent -> enrichWithScientificName(imageEvent, speciesById)));
                });
    }

    @Override
    public Flux<MapSighting> findDoneSightingsInBounds(BigDecimal minLat, BigDecimal maxLat, BigDecimal minLng, BigDecimal maxLng) {
        log.info("Finding done sightings in bounds lat [{},{}] lng [{},{}]", minLat, maxLat, minLng, maxLng);
        return imageEventR2DBCRepository.findDoneSightingsInBounds(minLat, maxLat, minLng, maxLng)
                .map(imageEventMapper::toModelFromProjection)
                .collectList()
                .flatMapMany(sightings -> {
                    List<UUID> specieIds = sightings.stream().map(MapSighting::getSpeciesId).toList();
                    return specieCacheService.findAllByIds(specieIds)
                            .flatMapMany(speciesById -> Flux.fromIterable(sightings)
                                    .map(sighting -> enrichWithScientificName(sighting, speciesById)));
                });
    }

    private ImageEvent enrichWithScientificName(ImageEvent imageEvent, Map<UUID, SpecieEntity> speciesById) {
        SpecieEntity specie = speciesById.get(imageEvent.getSpecieId());
        if (specie == null) {
            throw new SpecieNotFoundException("Not found By UUID: " + imageEvent.getSpecieId());
        }
        imageEvent.setScientificName(specie.getScientificName());
        return imageEvent;
    }

    private MapSighting enrichWithScientificName(MapSighting sighting, Map<UUID, SpecieEntity> speciesById) {
        SpecieEntity specie = speciesById.get(sighting.getSpeciesId());
        if (specie == null) {
            throw new SpecieNotFoundException("Not found By UUID: " + sighting.getSpeciesId());
        }
        sighting.setScientificName(specie.getScientificName());
        return sighting;
    }

}
