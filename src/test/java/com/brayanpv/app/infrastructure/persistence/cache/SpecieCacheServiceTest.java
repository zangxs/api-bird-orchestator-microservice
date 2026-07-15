package com.brayanpv.app.infrastructure.persistence.cache;

import com.brayanpv.app.infrastructure.persistence.entity.SpecieEntity;
import com.brayanpv.app.infrastructure.persistence.repository.ISpecieR2DBCRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Plain unit tests verifying delegation only - the {@code @Cacheable} behavior itself requires a
 * Spring proxy and isn't exercised by direct instantiation.
 */
@ExtendWith(MockitoExtension.class)
class SpecieCacheServiceTest {

    @Mock
    private ISpecieR2DBCRepository specieR2DBCRepository;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache speciesByIdCache;

    private SpecieCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new SpecieCacheService(specieR2DBCRepository, cacheManager);
    }

    @Test
    void findByScientificName_delegatesToRepository() {
        SpecieEntity specie = SpecieEntity.builder().id(UUID.randomUUID()).scientificName("Turdus merula").build();
        when(specieR2DBCRepository.findByScientificName("Turdus merula")).thenReturn(Mono.just(specie));

        StepVerifier.create(cacheService.findByScientificName("Turdus merula"))
                .expectNext(specie)
                .verifyComplete();
    }

    @Test
    void findById_delegatesToRepository() {
        UUID id = UUID.randomUUID();
        SpecieEntity specie = SpecieEntity.builder().id(id).build();
        when(specieR2DBCRepository.findById(id)).thenReturn(Mono.just(specie));

        StepVerifier.create(cacheService.findById(id))
                .expectNext(specie)
                .verifyComplete();
    }

    @Test
    void findAllByIds_whenEmptyCollection_returnsEmptyMapWithoutTouchingCacheOrRepository() {
        StepVerifier.create(cacheService.findAllByIds(List.of()))
                .expectNext(Map.of())
                .verifyComplete();

        verify(cacheManager, never()).getCache(any());
        verify(specieR2DBCRepository, never()).findAllById(anyCollection());
    }

    @Test
    void findAllByIds_whenAllIdsAreCached_resolvesFromCacheWithoutQueryingRepository() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        SpecieEntity specie1 = SpecieEntity.builder().id(id1).scientificName("Turdus merula").build();
        SpecieEntity specie2 = SpecieEntity.builder().id(id2).scientificName("Passer domesticus").build();

        when(cacheManager.getCache("speciesById")).thenReturn(speciesByIdCache);
        when(speciesByIdCache.get(id1, SpecieEntity.class)).thenReturn(specie1);
        when(speciesByIdCache.get(id2, SpecieEntity.class)).thenReturn(specie2);

        StepVerifier.create(cacheService.findAllByIds(List.of(id1, id2)))
                .assertNext(result -> assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(id1, specie1, id2, specie2)))
                .verifyComplete();

        verify(specieR2DBCRepository, never()).findAllById(anyCollection());
    }

    @Test
    void findAllByIds_whenAllIdsAreMissing_batchFetchesAndPopulatesCache() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        SpecieEntity specie1 = SpecieEntity.builder().id(id1).scientificName("Turdus merula").build();
        SpecieEntity specie2 = SpecieEntity.builder().id(id2).scientificName("Passer domesticus").build();

        when(cacheManager.getCache("speciesById")).thenReturn(speciesByIdCache);
        when(speciesByIdCache.get(any(UUID.class), org.mockito.ArgumentMatchers.eq(SpecieEntity.class))).thenReturn(null);
        when(specieR2DBCRepository.findAllById(anyCollection())).thenReturn(Flux.just(specie1, specie2));

        StepVerifier.create(cacheService.findAllByIds(List.of(id1, id2)))
                .assertNext(result -> assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(id1, specie1, id2, specie2)))
                .verifyComplete();

        verify(speciesByIdCache).put(id1, specie1);
        verify(speciesByIdCache).put(id2, specie2);
    }

    @Test
    void findAllByIds_whenSomeIdsAreCachedAndSomeAreNot_onlyQueriesTheMisses() {
        UUID cachedId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        SpecieEntity cachedSpecie = SpecieEntity.builder().id(cachedId).scientificName("Turdus merula").build();
        SpecieEntity fetchedSpecie = SpecieEntity.builder().id(missingId).scientificName("Passer domesticus").build();

        when(cacheManager.getCache("speciesById")).thenReturn(speciesByIdCache);
        when(speciesByIdCache.get(cachedId, SpecieEntity.class)).thenReturn(cachedSpecie);
        when(speciesByIdCache.get(missingId, SpecieEntity.class)).thenReturn(null);
        when(specieR2DBCRepository.findAllById(List.of(missingId))).thenReturn(Flux.just(fetchedSpecie));

        StepVerifier.create(cacheService.findAllByIds(List.of(cachedId, missingId)))
                .assertNext(result -> assertThat(result)
                        .containsExactlyInAnyOrderEntriesOf(Map.of(cachedId, cachedSpecie, missingId, fetchedSpecie)))
                .verifyComplete();

        verify(speciesByIdCache, never()).put(cachedId, cachedSpecie);
        verify(speciesByIdCache).put(missingId, fetchedSpecie);
    }

    @Test
    void findAllByIds_deduplicatesRepeatedIds() {
        UUID id = UUID.randomUUID();
        SpecieEntity specie = SpecieEntity.builder().id(id).scientificName("Turdus merula").build();

        when(cacheManager.getCache("speciesById")).thenReturn(speciesByIdCache);
        when(speciesByIdCache.get(id, SpecieEntity.class)).thenReturn(null);
        when(specieR2DBCRepository.findAllById(List.of(id))).thenReturn(Flux.just(specie));

        StepVerifier.create(cacheService.findAllByIds(List.of(id, id, id)))
                .assertNext(result -> assertThat(result).containsExactlyEntriesOf(Map.of(id, specie)))
                .verifyComplete();
    }
}
