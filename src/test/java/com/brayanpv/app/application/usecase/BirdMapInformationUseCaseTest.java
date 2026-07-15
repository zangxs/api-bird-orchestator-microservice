package com.brayanpv.app.application.usecase;

import com.brayanpv.app.domain.model.MapSighting;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.domain.storage.IImageStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BirdMapInformationUseCaseTest {

    @Mock
    private IImageEventRepository imageEventRepository;

    @Mock
    private IImageStoragePort imageStoragePort;

    private BirdMapInformationUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new BirdMapInformationUseCase(imageEventRepository, imageStoragePort);
    }

    @Test
    void execute_mapsSightingsWithPresignedThumbnailUrls() {
        BigDecimal minLat = BigDecimal.valueOf(-10), maxLat = BigDecimal.valueOf(10);
        BigDecimal minLng = BigDecimal.valueOf(-10), maxLng = BigDecimal.valueOf(10);

        MapSighting sighting = MapSighting.builder()
                .imageEventId(UUID.randomUUID())
                .speciesId(UUID.randomUUID())
                .s3Key("images/key.jpg")
                .commonName("Blackbird")
                .scientificName("Turdus merula")
                .latitude(BigDecimal.valueOf(1))
                .longitude(BigDecimal.valueOf(2))
                .build();

        when(imageEventRepository.findDoneSightingsInBounds(minLat, maxLat, minLng, maxLng))
                .thenReturn(Flux.just(sighting));
        when(imageStoragePort.generatePresignedUrl(eq("images/key.jpg"), any(Duration.class)))
                .thenReturn(Mono.just("https://s3/presigned-url"));

        StepVerifier.create(useCase.execute(minLat, maxLat, minLng, maxLng))
                .assertNext(response -> {
                    assertThat(response.getImageEventId()).isEqualTo(sighting.getImageEventId());
                    assertThat(response.getScientificName()).isEqualTo("Turdus merula");
                    assertThat(response.getThumbnailUrl()).isEqualTo("https://s3/presigned-url");
                })
                .verifyComplete();
    }

    @Test
    void execute_whenNoSightingsInBounds_returnsEmptyFlux() {
        BigDecimal bound = BigDecimal.ZERO;
        when(imageEventRepository.findDoneSightingsInBounds(bound, bound, bound, bound)).thenReturn(Flux.empty());

        StepVerifier.create(useCase.execute(bound, bound, bound, bound)).verifyComplete();
    }

    @Test
    void execute_whenPresignedUrlGenerationFails_propagatesError() {
        BigDecimal bound = BigDecimal.ZERO;
        MapSighting sighting = MapSighting.builder().s3Key("images/key.jpg").build();

        when(imageEventRepository.findDoneSightingsInBounds(bound, bound, bound, bound)).thenReturn(Flux.just(sighting));
        when(imageStoragePort.generatePresignedUrl(any(), any())).thenReturn(Mono.error(new RuntimeException("s3 down")));

        StepVerifier.create(useCase.execute(bound, bound, bound, bound))
                .expectErrorMessage("s3 down")
                .verify();
    }
}
