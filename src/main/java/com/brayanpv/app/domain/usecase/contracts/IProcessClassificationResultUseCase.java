package com.brayanpv.app.domain.usecase.contracts;

import com.brayanpv.app.domain.model.BirdClassificationResult;
import reactor.core.publisher.Mono;

public interface IProcessClassificationResultUseCase {

    Mono<Void> execute(BirdClassificationResult result);
}
