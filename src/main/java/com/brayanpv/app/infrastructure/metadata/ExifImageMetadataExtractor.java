package com.brayanpv.app.infrastructure.metadata;

import com.brayanpv.app.domain.metadata.IImageMetadataExtractor;
import com.brayanpv.app.domain.model.GeoLocation;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;

@Component
@Log4j2
public class ExifImageMetadataExtractor implements IImageMetadataExtractor {
    @Override
    public Mono<GeoLocation> extractGeoLocation(byte[] imageBytes) {
        log.info("extractGeoLocation from bytes");
        return Mono.fromCallable(() -> extractSync(imageBytes))
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorResume(e -> {
                    log.warn("No se pudo extraer EXIF de la imagen: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private GeoLocation extractSync(byte[] imageBytes) throws ImageProcessingException, IOException {
        Metadata metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageBytes));
        GpsDirectory gpsDirectory = metadata.getFirstDirectoryOfType(GpsDirectory.class);

        if (gpsDirectory == null || gpsDirectory.getGeoLocation() == null) {
            return null; // sin GPS en el EXIF — comun en fotos comprimidas o de redes sociales
        }

        com.drew.lang.GeoLocation location = gpsDirectory.getGeoLocation();
        return GeoLocation.builder()
                .latitude(BigDecimal.valueOf(location.getLatitude()))
                .longitude(BigDecimal.valueOf(location.getLongitude()))
                .build();
    }

}
