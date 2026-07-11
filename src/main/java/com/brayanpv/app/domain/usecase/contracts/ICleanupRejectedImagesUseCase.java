package com.brayanpv.app.domain.usecase.contracts;

import reactor.core.publisher.Mono;

public interface ICleanupRejectedImagesUseCase {
    Mono<Void> execute();
}
