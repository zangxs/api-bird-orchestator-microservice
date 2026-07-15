package com.brayanpv.app.infrastructure.persistence.cache;

import com.brayanpv.app.infrastructure.persistence.entity.SpecieEntity;
import com.brayanpv.app.infrastructure.persistence.repository.ISpecieR2DBCRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Caches species lookups (species table changes rarely — reference data seeded via db/ scripts).
 * Kept as its own Spring bean rather than methods on {@link com.brayanpv.app.infrastructure.persistence.adapter.ImageEventRepository}
 * because {@code @Cacheable} relies on a proxy: it only intercepts calls made from outside the bean,
 * not self-invocations within the same class.
 */
@Service
@RequiredArgsConstructor
public class SpecieCacheService {

    private final ISpecieR2DBCRepository specieR2DBCRepository;

    @Cacheable(cacheNames = "speciesByScientificName", key = "#scientificName")
    public Mono<SpecieEntity> findByScientificName(String scientificName) {
        return specieR2DBCRepository.findByScientificName(scientificName);
    }

    @Cacheable(cacheNames = "speciesById", key = "#id")
    public Mono<SpecieEntity> findById(UUID id) {
        return specieR2DBCRepository.findById(id);
    }
}
