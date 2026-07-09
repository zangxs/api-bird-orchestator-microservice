package com.brayanpv.app.infrastructure.messaging.producer.implementations;

import com.brayanpv.app.domain.messaging.IEventPublisherPort;
import com.brayanpv.app.domain.model.BirdObserved;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Log4j2
public class BirdObservedEventPublisher implements IEventPublisherPort {

    private final RabbitTemplate rabbitTemplate;


    @Override
    public Mono<Void> publish(BirdObserved birdObserved) {
        return Mono.empty();
    }
}
