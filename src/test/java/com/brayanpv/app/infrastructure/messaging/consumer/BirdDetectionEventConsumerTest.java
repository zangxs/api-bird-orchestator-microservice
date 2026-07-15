package com.brayanpv.app.infrastructure.messaging.consumer;

import com.brayanpv.app.domain.model.BirdDetectionResult;
import com.brayanpv.app.domain.usecase.contracts.IProcessDetectionResultUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Delivery;
import com.rabbitmq.client.Envelope;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BirdDetectionEventConsumerTest {

    private static final String QUEUE = "bird_detection.resultado.queue";

    @Mock
    private IProcessDetectionResultUseCase processDetectionResultUseCase;

    @Mock
    private reactor.rabbitmq.Receiver receiver;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BirdDetectionEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new BirdDetectionEventConsumer(processDetectionResultUseCase, receiver, objectMapper);
        ReflectionTestUtils.setField(consumer, "resultQueueName", QUEUE);
    }

    private Delivery delivery(byte[] body) {
        return new Delivery(new Envelope(1L, false, "exchange", "routing-key"), null, body);
    }

    @Test
    void start_deserializesDeliveryAndDelegatesToUseCase() throws Exception {
        UUID imageEventId = UUID.randomUUID();
        BirdDetectionResult result = new BirdDetectionResult();
        result.setImageEventId(imageEventId);
        result.setBird(true);
        result.setConfidence(BigDecimal.valueOf(0.9));
        byte[] body = objectMapper.writeValueAsBytes(result);

        when(receiver.consumeAutoAck(QUEUE)).thenReturn(Flux.just(delivery(body)));
        when(processDetectionResultUseCase.execute(any(BirdDetectionResult.class))).thenReturn(Mono.empty());

        // No scheduler hop happens in this pipeline (Flux.just + Mono.fromCallable + flatMap all
        // run on the subscribing thread), so consumer.start()'s internal .subscribe() completes
        // synchronously - no need to wait for it.
        consumer.start();

        verify(processDetectionResultUseCase).execute(any(BirdDetectionResult.class));
    }

    @Test
    void start_whenPayloadIsNotValidJson_doesNotPropagateErrorAndKeepsSubscriptionAlive() {
        when(receiver.consumeAutoAck(QUEUE))
                .thenReturn(Flux.just(delivery("not-json".getBytes(StandardCharsets.UTF_8))));

        consumer.start();

        verify(processDetectionResultUseCase, org.mockito.Mockito.never()).execute(any());
    }
}
