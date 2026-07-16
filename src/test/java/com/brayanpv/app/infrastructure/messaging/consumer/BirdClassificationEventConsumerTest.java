package com.brayanpv.app.infrastructure.messaging.consumer;

import com.brayanpv.app.domain.model.BirdClassificationResult;
import com.brayanpv.app.domain.usecase.contracts.IProcessClassificationResultUseCase;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BirdClassificationEventConsumerTest {

    private static final String QUEUE = "bird_classification.result.queue";

    @Mock
    private IProcessClassificationResultUseCase processClassificationResultUseCase;

    @Mock
    private reactor.rabbitmq.Receiver receiver;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BirdClassificationEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new BirdClassificationEventConsumer(processClassificationResultUseCase, receiver, objectMapper, Mono.empty());
        ReflectionTestUtils.setField(consumer, "classificationResultQueueName", QUEUE);
    }

    private Delivery delivery(byte[] body) {
        return new Delivery(new Envelope(1L, false, "exchange", "routing-key"), null, body);
    }

    @Test
    void start_deserializesDeliveryAndDelegatesToUseCase() throws Exception {
        BirdClassificationResult result = new BirdClassificationResult();
        result.setImageEventId(UUID.randomUUID());
        result.setScientificName("Turdus merula");
        byte[] body = objectMapper.writeValueAsBytes(result);

        when(receiver.consumeAutoAck(QUEUE)).thenReturn(Flux.just(delivery(body)));
        when(processClassificationResultUseCase.execute(any(BirdClassificationResult.class))).thenReturn(Mono.empty());

        consumer.start();

        verify(processClassificationResultUseCase).execute(any(BirdClassificationResult.class));
    }

    @Test
    void start_whenPayloadIsNotValidJson_doesNotPropagateErrorAndKeepsSubscriptionAlive() {
        when(receiver.consumeAutoAck(QUEUE))
                .thenReturn(Flux.just(delivery("not-json".getBytes(StandardCharsets.UTF_8))));

        consumer.start();

        verify(processClassificationResultUseCase, never()).execute(any());
    }
}
