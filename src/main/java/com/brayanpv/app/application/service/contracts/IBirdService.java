package com.brayanpv.app.application.service.contracts;

import com.brayanpv.app.application.dto.request.ImageUploadRequest;
import com.brayanpv.app.application.dto.response.ImageUploadResponse;
import reactor.core.publisher.Mono;

public interface IBirdService {

    Mono<ImageUploadResponse> processImage(ImageUploadRequest imageUploadRequest);
}
