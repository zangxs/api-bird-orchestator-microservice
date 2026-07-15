package com.brayanpv.app.infrastructure.messaging.broker;

import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.UUID;

class InMemoryImageEventResultBrokerTest {

    private final InMemoryImageEventResultBroker broker = new InMemoryImageEventResultBroker();

    @Test
    void awaitResult_whenCompletedBeforeTimeout_emitsTheCompletedEvent() {
        UUID imageEventId = UUID.randomUUID();
        ImageEvent resolved = ImageEvent.builder().id(imageEventId).status(ImageStatus.BIRD_DETECTED).build();

        var resultMono = broker.awaitResult(imageEventId, Duration.ofSeconds(2));
        broker.complete(resolved);

        StepVerifier.create(resultMono)
                .expectNext(resolved)
                .verifyComplete();
    }

    @Test
    void awaitResult_whenNeverCompleted_timesOutToEmpty() {
        UUID imageEventId = UUID.randomUUID();

        StepVerifier.create(broker.awaitResult(imageEventId, Duration.ofMillis(100)))
                .verifyComplete();
    }

    @Test
    void complete_forUnknownImageEventId_isANoOp() {
        ImageEvent unrelated = ImageEvent.builder().id(UUID.randomUUID()).status(ImageStatus.DONE).build();

        // No waiter registered for this id - complete() must not throw.
        broker.complete(unrelated);
    }
}
