package com.brayanpv.app.infrastructure.persistence.repository;

import com.brayanpv.app.infrastructure.persistence.entity.SpecieEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface ISpecieR2DBCRepository extends ReactiveCrudRepository<SpecieEntity, UUID> {

    Mono<SpecieEntity> findByScientificName(String scientificName);
}
