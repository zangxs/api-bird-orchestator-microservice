package com.brayanpv.app.domain.messaging;

import com.brayanpv.app.domain.model.BirdObserved;
import reactor.core.publisher.Mono;

public interface IEventPublisherPort {

    Mono<Void> publish(BirdObserved birdObserved);
}
