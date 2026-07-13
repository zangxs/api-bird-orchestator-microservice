package com.brayanpv.app.application.service.contracts;

import com.brayanpv.app.application.dto.request.ImageUploadRequest;
import com.brayanpv.app.application.dto.response.ImageStatusResponse;
import com.brayanpv.app.application.dto.response.ImageUploadResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IBirdService {

    Mono<ImageUploadResponse> processImage(ImageUploadRequest imageUploadRequest);
    Mono<ImageStatusResponse> getImageStatus(UUID imageEventId);
}
