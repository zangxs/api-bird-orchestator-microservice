package com.brayanpv.app.infrastructure.messaging.producer;

import com.brayanpv.app.domain.messaging.IEventPublisherPort;
import com.brayanpv.app.domain.model.BirdObserved;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;

@Component
@RequiredArgsConstructor
@Log4j2
public class BirdObservedEventPublisher implements IEventPublisherPort {

    private final Sender sender;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key}")
    private String routingKey;

    @Override
    public Mono<Void> publish(BirdObserved birdObserved) {
        log.info("Publishing bird observed {}", birdObserved);

        return Mono.fromCallable(() -> objectMapper.writeValueAsBytes(birdObserved))
                .flatMap(body -> {
                    OutboundMessage message = new OutboundMessage(exchange, routingKey, body);
                    return sender.send(Mono.just(message));
                })
                .doOnSuccess(v -> log.info("Published bird observed {}", birdObserved))
                .doOnError(e -> log.error("Failed to publish bird observed {}", birdObserved, e));
    }

}
