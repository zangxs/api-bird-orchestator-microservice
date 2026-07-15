package com.brayanpv.app.domain.usecase.contracts;

import com.brayanpv.app.domain.model.ImageEvent;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface IGetImageEventUseCase {
    Flux<ImageEvent> getImageEvents(UUID userId);
}
