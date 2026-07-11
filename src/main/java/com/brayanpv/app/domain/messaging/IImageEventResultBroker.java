package com.brayanpv.app.domain.messaging;

import com.brayanpv.app.domain.model.ImageEvent;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

public interface IImageEventResultBroker {

    Mono<ImageEvent> awaitResult(UUID imageEventId, Duration timeout);
    void complete(ImageEvent imageEvent);
}
