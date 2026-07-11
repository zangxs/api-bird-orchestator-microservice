package com.brayanpv.app.domain.usecase.contracts;

import com.brayanpv.app.domain.model.BirdDetectionResult;
import reactor.core.publisher.Mono;

public interface IProcessDetectionResultUseCase {
    Mono<Void> execute(BirdDetectionResult result);
}
