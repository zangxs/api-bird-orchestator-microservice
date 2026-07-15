package com.brayanpv.app.application.usecase;

import com.brayanpv.app.domain.messaging.IEventPublisherPort;
import com.brayanpv.app.domain.messaging.IImageEventResultBroker;
import com.brayanpv.app.domain.model.BirdClassificationResult;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.ManualClassificationRequested;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessClassificationResultUseCaseTest {

    private static final String MANUAL_ROUTING_KEY = "bird_classification.manual.pending";

    @Mock
    private IImageEventRepository imageEventRepository;

    @Mock
    private IEventPublisherPort eventPublisherPort;

    @Mock
    private IImageEventResultBroker resultBroker;

    private ProcessClassificationResultUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessClassificationResultUseCase(imageEventRepository, eventPublisherPort, resultBroker);
        ReflectionTestUtils.setField(useCase, "manualClassificationRoutingKey", MANUAL_ROUTING_KEY);
    }

    @Test
    void execute_whenClassificationSucceeds_completesBrokerAndDoesNotPublish() {
        UUID imageEventId = UUID.randomUUID();
        BirdClassificationResult result = new BirdClassificationResult();
        result.setImageEventId(imageEventId);
        result.setScientificName("Turdus merula");
        result.setSpecieConfidence(BigDecimal.valueOf(0.95));

        ImageEvent updated = ImageEvent.builder()
                .id(imageEventId)
                .status(ImageStatus.DONE)
                .build();
        when(imageEventRepository.updateClassification(imageEventId, "Turdus merula", result.getSpecieConfidence(), null))
                .thenReturn(Mono.just(updated));

        StepVerifier.create(useCase.execute(result)).verifyComplete();

        verify(resultBroker).complete(updated);
        verify(eventPublisherPort, never()).publish(any(), any());
    }

    @Test
    void execute_whenClassificationFails_publishesManualClassificationRequestedWithReason() {
        UUID imageEventId = UUID.randomUUID();
        BirdClassificationResult result = new BirdClassificationResult();
        result.setImageEventId(imageEventId);
        result.setFailureReason("low confidence");

        ImageEvent updated = ImageEvent.builder()
                .id(imageEventId)
                .s3Key("images/key.jpg")
                .status(ImageStatus.FAILED)
                .failureReason("low confidence")
                .build();
        when(imageEventRepository.updateClassification(imageEventId, null, null, "low confidence"))
                .thenReturn(Mono.just(updated));
        when(eventPublisherPort.publish(eq(MANUAL_ROUTING_KEY), any(ManualClassificationRequested.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(result)).verifyComplete();

        ArgumentCaptor<ManualClassificationRequested> captor = ArgumentCaptor.forClass(ManualClassificationRequested.class);
        verify(eventPublisherPort).publish(eq(MANUAL_ROUTING_KEY), captor.capture());
        assertThat(captor.getValue().getImageEventId()).isEqualTo(imageEventId);
        assertThat(captor.getValue().getS3Key()).isEqualTo("images/key.jpg");
        assertThat(captor.getValue().getReason()).isEqualTo("low confidence");
        verify(resultBroker, never()).complete(any());
    }

    @Test
    void execute_whenRepositoryFails_propagatesError() {
        UUID imageEventId = UUID.randomUUID();
        BirdClassificationResult result = new BirdClassificationResult();
        result.setImageEventId(imageEventId);

        when(imageEventRepository.updateClassification(eq(imageEventId), any(), any(), any()))
                .thenReturn(Mono.error(new RuntimeException("species not found")));

        StepVerifier.create(useCase.execute(result))
                .expectErrorMessage("species not found")
                .verify();
    }
}
