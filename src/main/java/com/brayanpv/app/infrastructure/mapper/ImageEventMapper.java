package com.brayanpv.app.infrastructure.mapper;

import com.brayanpv.app.domain.model.BirdDetectionResult;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.MapSighting;
import com.brayanpv.app.infrastructure.persistence.entity.ImageEventEntity;
import com.brayanpv.app.infrastructure.persistence.projection.MapSightingProjection;
import org.springframework.stereotype.Component;

@Component
public class ImageEventMapper {

    public ImageEventEntity toEntityFromModel(ImageEvent imageEvent) {

        return ImageEventEntity.builder()
                .id(imageEvent.getId())
                .status(imageEvent.getStatus())
                .s3Key(imageEvent.getS3Key())
                .userId(imageEvent.getUserId())
                .latitude(imageEvent.getLatitude())
                .longitude(imageEvent.getLongitude())
                .build();
    }


    public ImageEvent toModelFromEntity(ImageEventEntity imageEventEntity) {
        return ImageEvent.builder()
                .userId(imageEventEntity.getUserId())
                .id(imageEventEntity.getId())
                .status(imageEventEntity.getStatus())
                .s3Key(imageEventEntity.getS3Key())
                .birdConfidence(imageEventEntity.getBirdConfidence())
                .specieConfidence(imageEventEntity.getSpeciesConfidence())
                .specieId(imageEventEntity.getSpeciesId())
                .failureReason(imageEventEntity.getFailureReason())
                .build();
    }

    public MapSighting toModelFromProjection(MapSightingProjection projection) {
        return MapSighting.builder()
                .imageEventId(projection.getImageEventId())
                .speciesId(projection.getSpeciesId())
                .s3Key(projection.getS3Key())
                .commonName(projection.getCommonName())
                .scientificName(projection.getScientificName())
                .latitude(projection.getLatitude())
                .longitude(projection.getLongitude())
                .build();
    }

}
