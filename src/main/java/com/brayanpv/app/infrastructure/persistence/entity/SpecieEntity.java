package com.brayanpv.app.infrastructure.persistence.entity;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Table("species")
@Data
@Builder
public class SpecieEntity {

    @Id
    private UUID id;
    @Column("common_name")
    private String commonName;
    @Column("scientific_name")
    private String scientificName;
    @Column("common_name_en")
    private String commonNameEn;
    @Column("family")
    private UUID familyId;
    @Column("image_url")
    private String imageUrl;
    private String description;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column("deleted_at")
    private LocalDateTime deletedAt;
}
