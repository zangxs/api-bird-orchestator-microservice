package com.brayanpv.app.application.usecase.contracts;

import com.brayanpv.app.application.dto.request.ImageUploadRequest;
import com.brayanpv.app.application.dto.response.ImageUploadResponse;
import reactor.core.publisher.Mono;

public interface IProcessBirdImageUseCase {
    Mono<ImageUploadResponse> execute(ImageUploadRequest imageUploadRequest);
}
