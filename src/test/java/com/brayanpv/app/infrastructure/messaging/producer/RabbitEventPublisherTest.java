package com.brayanpv.app.infrastructure.messaging.producer;

import com.brayanpv.app.domain.model.BirdObserved;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivestreams.Publisher;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.rabbitmq.OutboundMessage;
import reactor.rabbitmq.Sender;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RabbitEventPublisherTest {

    private static final String EXCHANGE = "bird_detection.exchange";

    @Mock
    private Sender sender;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RabbitEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RabbitEventPublisher(sender, objectMapper);
        ReflectionTestUtils.setField(publisher, "exchange", EXCHANGE);
    }

    @Test
    void publish_sendsMessageToConfiguredExchangeWithGivenRoutingKey() {
        when(sender.send(any(Publisher.class))).thenReturn(Mono.empty());

        BirdObserved payload = BirdObserved.builder()
                .imageEventId(UUID.randomUUID())
                .s3Key("images/key.jpg")
                .status("PROCESSING")
                .build();

        StepVerifier.create(publisher.publish("bird_detection.pending", payload)).verifyComplete();

        ArgumentCaptor<Publisher<OutboundMessage>> captor = ArgumentCaptor.forClass(Publisher.class);
        org.mockito.Mockito.verify(sender).send(captor.capture());
        OutboundMessage message = Mono.from(captor.getValue()).block();
        assertThat(message).isNotNull();
        assertThat(message.getExchange()).isEqualTo(EXCHANGE);
        assertThat(message.getRoutingKey()).isEqualTo("bird_detection.pending");
    }

    @Test
    void publish_whenSerializationFails_propagatesError() {
        // A plain Object has no properties and isn't a bean Jackson knows how to introspect, so
        // writeValueAsBytes(...) throws (FAIL_ON_EMPTY_BEANS is on by default) instead of
        // publishing garbage to the exchange.
        StepVerifier.create(publisher.publish("bird_detection.pending", new Object()))
                .expectError()
                .verify();
    }
}
