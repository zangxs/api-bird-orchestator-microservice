package com.brayanpv.app.domain.usecase.contracts;

import com.brayanpv.app.application.dto.response.ImageStatusResponse;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface IGetImageStatusUseCase {
    Mono<ImageStatusResponse> execute(UUID imageEventId);
}
