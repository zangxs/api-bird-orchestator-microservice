package com.brayanpv.app.infrastructure.persistence.cache;

import com.brayanpv.app.infrastructure.persistence.entity.SpecieEntity;
import com.brayanpv.app.infrastructure.persistence.repository.ISpecieR2DBCRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    private static final String SPECIES_BY_ID_CACHE = "speciesById";

    private final ISpecieR2DBCRepository specieR2DBCRepository;
    private final CacheManager cacheManager;

    @Cacheable(cacheNames = "speciesByScientificName", key = "#scientificName")
    public Mono<SpecieEntity> findByScientificName(String scientificName) {
        return specieR2DBCRepository.findByScientificName(scientificName);
    }

    @Cacheable(cacheNames = SPECIES_BY_ID_CACHE, key = "#id")
    public Mono<SpecieEntity> findById(UUID id) {
        return specieR2DBCRepository.findById(id);
    }

    /**
     * Batched counterpart to {@link #findById(UUID)} for the "N rows -> N species" fan-out in
     * {@code ImageEventRepository} (map sightings, user sighting lists): resolves already-cached
     * ids straight from the speciesById Caffeine cache (a non-blocking lookup - see
     * {@link com.brayanpv.app.infrastructure.configuration.CacheConfig}'s async-cache-mode note)
     * and fetches only the cache misses in a single {@code findAllById(...)} query instead of one
     * query per row. Freshly-fetched entities are written back into the same cache used by
     * {@link #findById(UUID)}, so a later single-id lookup for the same species is still a hit.
     */
    public Mono<Map<UUID, SpecieEntity>> findAllByIds(Collection<UUID> ids) {
        Set<UUID> uniqueIds = new HashSet<>(ids);
        if (uniqueIds.isEmpty()) {
            return Mono.just(Map.of());
        }

        Cache cache = cacheManager.getCache(SPECIES_BY_ID_CACHE);
        Map<UUID, SpecieEntity> result = new HashMap<>();
        List<UUID> missingIds = new ArrayList<>();

        for (UUID id : uniqueIds) {
            SpecieEntity cached = cache != null ? cache.get(id, SpecieEntity.class) : null;
            if (cached != null) {
                result.put(id, cached);
            } else {
                missingIds.add(id);
            }
        }

        if (missingIds.isEmpty()) {
            return Mono.just(result);
        }

        return specieR2DBCRepository.findAllById(missingIds)
                .doOnNext(specie -> {
                    if (cache != null) {
                        cache.put(specie.getId(), specie);
                    }
                    result.put(specie.getId(), specie);
                })
                .then(Mono.just(result));
    }
}
