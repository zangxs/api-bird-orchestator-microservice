package com.brayanpv.app.infrastructure.persistence.repository;

import com.brayanpv.app.infrastructure.persistence.entity.ImageEventEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

import java.util.UUID;

public interface IImageEventR2DBCRepository extends ReactiveCrudRepository<ImageEventEntity, UUID> {
}
