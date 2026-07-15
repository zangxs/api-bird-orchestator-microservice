package com.brayanpv.app.application.usecase;

import com.brayanpv.app.application.dto.response.MapSightingResponse;
import com.brayanpv.app.application.usecase.contracts.IBirdMapInformationUseCase;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.domain.storage.IImageStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Log4j2
public class BirdMapInformationUseCase implements IBirdMapInformationUseCase {

    private static final Duration THUMBNAIL_URL_DURATION = Duration.ofMinutes(30);

    private final IImageEventRepository imageEventRepository;
    private final IImageStoragePort imageStoragePort;

    @Override
    public Flux<MapSightingResponse> execute(BigDecimal minLat, BigDecimal maxLat, BigDecimal minLng, BigDecimal maxLng) {
        log.info("Getting bird map information for bounds lat [{},{}] lng [{},{}]", minLat, maxLat, minLng, maxLng);
        return imageEventRepository.findDoneSightingsInBounds(minLat, maxLat, minLng, maxLng)
                .flatMap(sighting -> imageStoragePort.generatePresignedUrl(sighting.getS3Key(), THUMBNAIL_URL_DURATION)
                        .map(thumbnailUrl -> MapSightingResponse.builder()
                                .imageEventId(sighting.getImageEventId())
                                .speciesId(sighting.getSpeciesId())
                                .commonName(sighting.getCommonName())
                                .scientificName(sighting.getScientificName())
                                .thumbnailUrl(thumbnailUrl)
                                .latitude(sighting.getLatitude())
                                .longitude(sighting.getLongitude())
                                .build()));
    }
}
