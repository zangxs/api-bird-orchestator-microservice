package com.brayanpv.app.infrastructure.persistence.repository;

import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.infrastructure.persistence.entity.ImageEventEntity;
import com.brayanpv.app.infrastructure.persistence.projection.MapSightingProjection;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.UUID;

public interface IImageEventR2DBCRepository extends ReactiveCrudRepository<ImageEventEntity, UUID> {
    Flux<ImageEventEntity> findByStatus(ImageStatus status);
    Flux<ImageEventEntity> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId,  ImageStatus status);

    @Query("""
        SELECT ie.id AS image_event_id,
               ie.user_id,
               ie.species_id,
               ie.s3_key,
               s.common_name,
               s.scientific_name,
               ie.latitude,
               ie.longitude
        FROM image_event ie
        JOIN species s ON s.id = ie.species_id
        WHERE ie.status = 'DONE'
          AND ie.latitude IS NOT NULL
          AND ie.longitude IS NOT NULL
          AND ie.latitude BETWEEN :minLat AND :maxLat
          AND ie.longitude BETWEEN :minLng AND :maxLng
        LIMIT 500
        """)
    Flux<MapSightingProjection> findDoneSightingsInBounds(
            BigDecimal minLat, BigDecimal maxLat,
            BigDecimal minLng, BigDecimal maxLng
    );
}
