package com.brayanpv.app.domain.metadata;

import com.brayanpv.app.domain.model.GeoLocation;
import reactor.core.publisher.Mono;

public interface IImageMetadataExtractor {
    Mono<GeoLocation> extractGeoLocation(byte[] imageBytes);

}
