package com.brayanpv.app.infrastructure.persistence.cache;

import com.brayanpv.app.infrastructure.persistence.entity.SpecieEntity;
import com.brayanpv.app.infrastructure.persistence.repository.ISpecieR2DBCRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.when;

/**
 * Plain unit tests verifying delegation only - the {@code @Cacheable} behavior itself requires a
 * Spring proxy and isn't exercised by direct instantiation.
 */
@ExtendWith(MockitoExtension.class)
class SpecieCacheServiceTest {

    @Mock
    private ISpecieR2DBCRepository specieR2DBCRepository;

    private SpecieCacheService cacheService;

    @BeforeEach
    void setUp() {
        cacheService = new SpecieCacheService(specieR2DBCRepository);
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
}
