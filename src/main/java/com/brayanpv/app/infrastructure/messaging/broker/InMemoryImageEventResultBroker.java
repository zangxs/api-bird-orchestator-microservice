package com.brayanpv.app.infrastructure.messaging.broker;

import com.brayanpv.app.domain.messaging.IImageEventResultBroker;
import com.brayanpv.app.domain.model.ImageEvent;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

@Component
@Log4j2
public class InMemoryImageEventResultBroker implements IImageEventResultBroker {

    private final Map<UUID, Sinks.One<ImageEvent>> pendingResults = new ConcurrentHashMap<>();


    @Override
    public Mono<ImageEvent> awaitResult(UUID imageEventId, Duration timeout) {
        Sinks.One<ImageEvent> sink = Sinks.one();
        pendingResults.put(imageEventId, sink);

        return sink.asMono()
                .timeout(timeout)
                .onErrorResume(e -> Mono.empty())
                .doFinally(signal -> pendingResults.remove(imageEventId));
    }

    @Override
    public void complete(ImageEvent imageEvent) {
        Sinks.One<ImageEvent> sink = pendingResults.get(imageEvent.getId());
        if (sink != null) {
            log.info("Completing broker wait for {} with status {}", imageEvent.getId(), imageEvent.getStatus());
            sink.tryEmitValue(imageEvent);
        }
    }
}
