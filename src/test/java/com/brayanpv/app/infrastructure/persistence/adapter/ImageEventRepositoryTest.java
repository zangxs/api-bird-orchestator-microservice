package com.brayanpv.app.infrastructure.persistence.adapter;

import com.brayanpv.app.domain.exception.ImageEventNotFoundException;
import com.brayanpv.app.domain.exception.SpecieNotFoundException;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.MapSighting;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.infrastructure.mapper.ImageEventMapper;
import com.brayanpv.app.infrastructure.persistence.cache.SpecieCacheService;
import com.brayanpv.app.infrastructure.persistence.entity.ImageEventEntity;
import com.brayanpv.app.infrastructure.persistence.entity.SpecieEntity;
import com.brayanpv.app.infrastructure.persistence.projection.MapSightingProjection;
import com.brayanpv.app.infrastructure.persistence.repository.IImageEventR2DBCRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageEventRepositoryTest {

    @Mock
    private IImageEventR2DBCRepository imageEventR2DBCRepository;

    @Mock
    private ImageEventMapper imageEventMapper;

    @Mock
    private SpecieCacheService specieCacheService;

    private ImageEventRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ImageEventRepository(imageEventR2DBCRepository, imageEventMapper, specieCacheService);
    }

    @Test
    void save_mapsModelToEntityAndBack() {
        ImageEvent model = ImageEvent.builder().userId(UUID.randomUUID()).status(ImageStatus.PROCESSING).build();
        ImageEventEntity entity = ImageEventEntity.builder().status(ImageStatus.PROCESSING).build();
        ImageEventEntity saved = ImageEventEntity.builder().id(UUID.randomUUID()).status(ImageStatus.PROCESSING).build();
        ImageEvent savedModel = ImageEvent.builder().id(saved.getId()).status(ImageStatus.PROCESSING).build();

        when(imageEventMapper.toEntityFromModel(model)).thenReturn(entity);
        when(imageEventR2DBCRepository.save(entity)).thenReturn(Mono.just(saved));
        when(imageEventMapper.toModelFromEntity(saved)).thenReturn(savedModel);

        StepVerifier.create(repository.save(model))
                .expectNext(savedModel)
                .verifyComplete();
    }

    @Test
    void updateDetection_whenIsBird_setsStatusBirdDetected() {
        UUID id = UUID.randomUUID();
        ImageEventEntity found = ImageEventEntity.builder().id(id).status(ImageStatus.PROCESSING).build();
        ImageEventEntity saved = ImageEventEntity.builder().id(id).status(ImageStatus.BIRD_DETECTED).build();
        ImageEvent mapped = ImageEvent.builder().id(id).status(ImageStatus.BIRD_DETECTED).build();

        when(imageEventR2DBCRepository.findById(id)).thenReturn(Mono.just(found));
        when(imageEventR2DBCRepository.save(found)).thenReturn(Mono.just(saved));
        when(imageEventMapper.toModelFromEntity(saved)).thenReturn(mapped);

        StepVerifier.create(repository.updateDetection(id, true, BigDecimal.valueOf(0.9)))
                .expectNext(mapped)
                .verifyComplete();

        assertThat(found.getStatus()).isEqualTo(ImageStatus.BIRD_DETECTED);
        assertThat(found.getBirdConfidence()).isEqualByComparingTo(BigDecimal.valueOf(0.9));
    }

    @Test
    void updateDetection_whenNotBird_setsStatusNotABird() {
        UUID id = UUID.randomUUID();
        ImageEventEntity found = ImageEventEntity.builder().id(id).status(ImageStatus.PROCESSING).build();

        when(imageEventR2DBCRepository.findById(id)).thenReturn(Mono.just(found));
        when(imageEventR2DBCRepository.save(found)).thenReturn(Mono.just(found));
        when(imageEventMapper.toModelFromEntity(found))
                .thenReturn(ImageEvent.builder().id(id).status(ImageStatus.NOT_A_BIRD).build());

        StepVerifier.create(repository.updateDetection(id, false, BigDecimal.valueOf(0.1)))
                .assertNext(event -> assertThat(event.getStatus()).isEqualTo(ImageStatus.NOT_A_BIRD))
                .verifyComplete();
    }

    @Test
    void updateDetection_whenNotFound_errorsWithImageEventNotFoundException() {
        UUID id = UUID.randomUUID();
        when(imageEventR2DBCRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(repository.updateDetection(id, true, BigDecimal.ONE))
                .expectError(ImageEventNotFoundException.class)
                .verify();
    }

    @Test
    void updateClassification_whenScientificNameIsNull_marksFailed() {
        UUID id = UUID.randomUUID();
        ImageEventEntity found = ImageEventEntity.builder().id(id).status(ImageStatus.IDENTIFYING).build();
        when(imageEventR2DBCRepository.findById(id)).thenReturn(Mono.just(found));
        when(imageEventR2DBCRepository.save(found)).thenReturn(Mono.just(found));
        when(imageEventMapper.toModelFromEntity(found))
                .thenReturn(ImageEvent.builder().id(id).status(ImageStatus.FAILED).failureReason("no match").build());

        StepVerifier.create(repository.updateClassification(id, null, null, "no match"))
                .assertNext(event -> {
                    assertThat(event.getStatus()).isEqualTo(ImageStatus.FAILED);
                    assertThat(event.getFailureReason()).isEqualTo("no match");
                })
                .verifyComplete();

        assertThat(found.getStatus()).isEqualTo(ImageStatus.FAILED);
        assertThat(found.getFailureReason()).isEqualTo("no match");
    }

    @Test
    void updateClassification_whenSpecieNotFound_errorsWithSpecieNotFoundException() {
        UUID id = UUID.randomUUID();
        ImageEventEntity found = ImageEventEntity.builder().id(id).status(ImageStatus.IDENTIFYING).build();
        when(imageEventR2DBCRepository.findById(id)).thenReturn(Mono.just(found));
        when(specieCacheService.findByScientificName("Unknown species")).thenReturn(Mono.empty());

        StepVerifier.create(repository.updateClassification(id, "Unknown species", BigDecimal.ONE, null))
                .expectError(SpecieNotFoundException.class)
                .verify();
    }

    @Test
    void updateClassification_whenSpecieFound_marksDone() {
        UUID id = UUID.randomUUID();
        UUID speciesId = UUID.randomUUID();
        ImageEventEntity found = ImageEventEntity.builder().id(id).status(ImageStatus.IDENTIFYING).build();
        SpecieEntity specie = SpecieEntity.builder().id(speciesId).scientificName("Turdus merula").build();

        when(imageEventR2DBCRepository.findById(id)).thenReturn(Mono.just(found));
        when(specieCacheService.findByScientificName("Turdus merula")).thenReturn(Mono.just(specie));
        when(imageEventR2DBCRepository.save(found)).thenReturn(Mono.just(found));
        when(imageEventMapper.toModelFromEntity(found))
                .thenReturn(ImageEvent.builder().id(id).status(ImageStatus.DONE).specieId(speciesId).build());

        StepVerifier.create(repository.updateClassification(id, "Turdus merula", BigDecimal.valueOf(0.8), null))
                .assertNext(event -> {
                    assertThat(event.getStatus()).isEqualTo(ImageStatus.DONE);
                    assertThat(event.getSpecieId()).isEqualTo(speciesId);
                })
                .verifyComplete();

        assertThat(found.getStatus()).isEqualTo(ImageStatus.DONE);
        assertThat(found.getSpeciesId()).isEqualTo(speciesId);
        assertThat(found.getSpeciesConfidence()).isEqualByComparingTo(BigDecimal.valueOf(0.8));
    }

    @Test
    void findByStatus_mapsAllEntitiesToModels() {
        ImageEventEntity e1 = ImageEventEntity.builder().id(UUID.randomUUID()).status(ImageStatus.NOT_A_BIRD).build();
        ImageEventEntity e2 = ImageEventEntity.builder().id(UUID.randomUUID()).status(ImageStatus.NOT_A_BIRD).build();
        when(imageEventR2DBCRepository.findByStatus(ImageStatus.NOT_A_BIRD)).thenReturn(Flux.just(e1, e2));
        when(imageEventMapper.toModelFromEntity(e1)).thenReturn(ImageEvent.builder().id(e1.getId()).build());
        when(imageEventMapper.toModelFromEntity(e2)).thenReturn(ImageEvent.builder().id(e2.getId()).build());

        StepVerifier.create(repository.findByStatus(ImageStatus.NOT_A_BIRD))
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void markExpired_whenFound_setsStatusExpired() {
        UUID id = UUID.randomUUID();
        ImageEventEntity found = ImageEventEntity.builder().id(id).status(ImageStatus.NOT_A_BIRD).build();
        when(imageEventR2DBCRepository.findById(id)).thenReturn(Mono.just(found));
        when(imageEventR2DBCRepository.save(found)).thenReturn(Mono.just(found));
        when(imageEventMapper.toModelFromEntity(found)).thenReturn(ImageEvent.builder().id(id).status(ImageStatus.EXPIRED).build());

        StepVerifier.create(repository.markExpired(id))
                .assertNext(event -> assertThat(event.getStatus()).isEqualTo(ImageStatus.EXPIRED))
                .verifyComplete();
    }

    @Test
    void markExpired_whenNotFound_errorsWithImageEventNotFoundException() {
        UUID id = UUID.randomUUID();
        when(imageEventR2DBCRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(repository.markExpired(id))
                .expectError(ImageEventNotFoundException.class)
                .verify();
    }

    @Test
    void findById_whenSpecieIdIsNull_skipsSpecieLookup() {
        UUID id = UUID.randomUUID();
        ImageEventEntity found = ImageEventEntity.builder().id(id).status(ImageStatus.PROCESSING).build();
        when(imageEventR2DBCRepository.findById(id)).thenReturn(Mono.just(found));
        when(imageEventMapper.toModelFromEntity(found)).thenReturn(ImageEvent.builder().id(id).status(ImageStatus.PROCESSING).build());

        StepVerifier.create(repository.findById(id))
                .assertNext(event -> assertThat(event.getScientificName()).isNull())
                .verifyComplete();
    }

    @Test
    void findById_whenSpecieIdPresent_enrichesWithScientificName() {
        UUID id = UUID.randomUUID();
        UUID specieId = UUID.randomUUID();
        ImageEventEntity found = ImageEventEntity.builder().id(id).status(ImageStatus.DONE).build();
        SpecieEntity specie = SpecieEntity.builder().id(specieId).scientificName("Turdus merula").build();

        when(imageEventR2DBCRepository.findById(id)).thenReturn(Mono.just(found));
        when(imageEventMapper.toModelFromEntity(found))
                .thenReturn(ImageEvent.builder().id(id).status(ImageStatus.DONE).specieId(specieId).build());
        when(specieCacheService.findById(specieId)).thenReturn(Mono.just(specie));

        StepVerifier.create(repository.findById(id))
                .assertNext(event -> assertThat(event.getScientificName()).isEqualTo("Turdus merula"))
                .verifyComplete();
    }

    @Test
    void findById_whenNotFound_errorsWithImageEventNotFoundException() {
        UUID id = UUID.randomUUID();
        when(imageEventR2DBCRepository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(repository.findById(id))
                .expectError(ImageEventNotFoundException.class)
                .verify();
    }

    @Test
    void findById_whenSpecieNotFound_errorsWithSpecieNotFoundException() {
        UUID id = UUID.randomUUID();
        UUID specieId = UUID.randomUUID();
        ImageEventEntity found = ImageEventEntity.builder().id(id).status(ImageStatus.DONE).build();

        when(imageEventR2DBCRepository.findById(id)).thenReturn(Mono.just(found));
        when(imageEventMapper.toModelFromEntity(found))
                .thenReturn(ImageEvent.builder().id(id).status(ImageStatus.DONE).specieId(specieId).build());
        when(specieCacheService.findById(specieId)).thenReturn(Mono.empty());

        StepVerifier.create(repository.findById(id))
                .expectError(SpecieNotFoundException.class)
                .verify();
    }

    @Test
    void findByUserId_enrichesEachSightingWithScientificName() {
        UUID userId = UUID.randomUUID();
        UUID specieId = UUID.randomUUID();
        ImageEventEntity entity = ImageEventEntity.builder().id(UUID.randomUUID()).status(ImageStatus.DONE).build();
        SpecieEntity specie = SpecieEntity.builder().id(specieId).scientificName("Passer domesticus").build();

        when(imageEventR2DBCRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, ImageStatus.DONE))
                .thenReturn(Flux.just(entity));
        when(imageEventMapper.toModelFromEntity(entity))
                .thenReturn(ImageEvent.builder().id(entity.getId()).specieId(specieId).status(ImageStatus.DONE).build());
        when(specieCacheService.findAllByIds(List.of(specieId))).thenReturn(Mono.just(Map.of(specieId, specie)));

        StepVerifier.create(repository.findByUserId(userId))
                .assertNext(event -> assertThat(event.getScientificName()).isEqualTo("Passer domesticus"))
                .verifyComplete();
    }

    @Test
    void findDoneSightingsInBounds_enrichesEachSightingWithScientificName() {
        UUID specieId = UUID.randomUUID();
        MapSightingProjection projection = new MapSightingProjection() {
            public UUID getId() { return UUID.randomUUID(); }
            public UUID getSpeciesId() { return specieId; }
            public String getS3Key() { return "images/key.jpg"; }
            public BigDecimal getLatitude() { return BigDecimal.valueOf(1); }
            public BigDecimal getLongitude() { return BigDecimal.valueOf(2); }
        };
        MapSighting mapped = MapSighting.builder().speciesId(specieId).s3Key("images/key.jpg").build();
        SpecieEntity specie = SpecieEntity.builder().id(specieId).scientificName("Cyanocitta cristata").build();

        BigDecimal minLat = BigDecimal.valueOf(0), maxLat = BigDecimal.valueOf(10);
        BigDecimal minLng = BigDecimal.valueOf(0), maxLng = BigDecimal.valueOf(10);

        when(imageEventR2DBCRepository.findDoneSightingsInBounds(minLat, maxLat, minLng, maxLng))
                .thenReturn(Flux.just(projection));
        when(imageEventMapper.toModelFromProjection(projection)).thenReturn(mapped);
        when(specieCacheService.findAllByIds(List.of(specieId))).thenReturn(Mono.just(Map.of(specieId, specie)));

        StepVerifier.create(repository.findDoneSightingsInBounds(minLat, maxLat, minLng, maxLng))
                .assertNext(sighting -> assertThat(sighting.getScientificName()).isEqualTo("Cyanocitta cristata"))
                .verifyComplete();
    }

    @Test
    void findDoneSightingsInBounds_whenSpecieNotFound_errorsWithSpecieNotFoundException() {
        UUID specieId = UUID.randomUUID();
        MapSightingProjection projection = new MapSightingProjection() {
            public UUID getId() { return UUID.randomUUID(); }
            public UUID getSpeciesId() { return specieId; }
            public String getS3Key() { return "images/key.jpg"; }
            public BigDecimal getLatitude() { return BigDecimal.valueOf(1); }
            public BigDecimal getLongitude() { return BigDecimal.valueOf(2); }
        };
        MapSighting mapped = MapSighting.builder().speciesId(specieId).s3Key("images/key.jpg").build();

        BigDecimal minLat = BigDecimal.valueOf(0), maxLat = BigDecimal.valueOf(10);
        BigDecimal minLng = BigDecimal.valueOf(0), maxLng = BigDecimal.valueOf(10);

        when(imageEventR2DBCRepository.findDoneSightingsInBounds(minLat, maxLat, minLng, maxLng))
                .thenReturn(Flux.just(projection));
        when(imageEventMapper.toModelFromProjection(projection)).thenReturn(mapped);
        when(specieCacheService.findAllByIds(List.of(specieId))).thenReturn(Mono.just(Map.of()));

        StepVerifier.create(repository.findDoneSightingsInBounds(minLat, maxLat, minLng, maxLng))
                .expectError(SpecieNotFoundException.class)
                .verify();
    }
}
