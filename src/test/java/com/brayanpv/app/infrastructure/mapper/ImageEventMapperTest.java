package com.brayanpv.app.infrastructure.mapper;

import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.MapSighting;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.infrastructure.persistence.entity.ImageEventEntity;
import com.brayanpv.app.infrastructure.persistence.projection.MapSightingProjection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ImageEventMapperTest {

    private final ImageEventMapper mapper = new ImageEventMapper();

    @Test
    void toEntityFromModel_copiesAllFields() {
        ImageEvent model = ImageEvent.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .s3Key("images/key.jpg")
                .status(ImageStatus.PROCESSING)
                .latitude(BigDecimal.valueOf(1.1))
                .longitude(BigDecimal.valueOf(2.2))
                .build();

        ImageEventEntity entity = mapper.toEntityFromModel(model);

        assertThat(entity.getId()).isEqualTo(model.getId());
        assertThat(entity.getUserId()).isEqualTo(model.getUserId());
        assertThat(entity.getS3Key()).isEqualTo(model.getS3Key());
        assertThat(entity.getStatus()).isEqualTo(model.getStatus());
        assertThat(entity.getLatitude()).isEqualByComparingTo(model.getLatitude());
        assertThat(entity.getLongitude()).isEqualByComparingTo(model.getLongitude());
    }

    @Test
    void toModelFromEntity_copiesAllFields() {
        ImageEventEntity entity = ImageEventEntity.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .s3Key("images/key.jpg")
                .status(ImageStatus.DONE)
                .birdConfidence(BigDecimal.valueOf(0.9))
                .speciesId(UUID.randomUUID())
                .speciesConfidence(BigDecimal.valueOf(0.8))
                .failureReason(null)
                .build();

        ImageEvent model = mapper.toModelFromEntity(entity);

        assertThat(model.getId()).isEqualTo(entity.getId());
        assertThat(model.getUserId()).isEqualTo(entity.getUserId());
        assertThat(model.getS3Key()).isEqualTo(entity.getS3Key());
        assertThat(model.getStatus()).isEqualTo(entity.getStatus());
        assertThat(model.getBirdConfidence()).isEqualByComparingTo(entity.getBirdConfidence());
        assertThat(model.getSpecieId()).isEqualTo(entity.getSpeciesId());
        assertThat(model.getSpecieConfidence()).isEqualByComparingTo(entity.getSpeciesConfidence());
    }

    @Test
    void toModelFromProjection_copiesAllFields() {
        UUID id = UUID.randomUUID();
        UUID speciesId = UUID.randomUUID();
        MapSightingProjection projection = new MapSightingProjection() {
            public UUID getId() { return id; }
            public UUID getSpeciesId() { return speciesId; }
            public String getS3Key() { return "images/key.jpg"; }
            public BigDecimal getLatitude() { return BigDecimal.valueOf(3.3); }
            public BigDecimal getLongitude() { return BigDecimal.valueOf(4.4); }
        };

        MapSighting sighting = mapper.toModelFromProjection(projection);

        assertThat(sighting.getImageEventId()).isEqualTo(id);
        assertThat(sighting.getSpeciesId()).isEqualTo(speciesId);
        assertThat(sighting.getS3Key()).isEqualTo("images/key.jpg");
        assertThat(sighting.getLatitude()).isEqualByComparingTo(BigDecimal.valueOf(3.3));
        assertThat(sighting.getLongitude()).isEqualByComparingTo(BigDecimal.valueOf(4.4));
    }
}
