package com.brayanpv.app.infrastructure.persistence.entity;

import com.brayanpv.app.domain.model.enums.ImageStatus;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("image_event")
@Data
@Builder
public class ImageEventEntity {
    @Id
    private UUID id;
    @Column("user_id")
    private UUID userId;
    @Column("s3_key")
    private String s3Key;
    @Column("status")
    private ImageStatus status;
    @Column("bird_confidence")
    private BigDecimal birdConfidence;
    @Column("species_id")
    private UUID speciesId;
    @Column("species_confidence")
    private BigDecimal speciesConfidence;
    @Column("latitude")
    private BigDecimal latitude;
    @Column("longitude")
    private BigDecimal longitude;
    @Column("failure_reason")
    private String failureReason;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column("deleted_at")
    private LocalDateTime deletedAt;

}
