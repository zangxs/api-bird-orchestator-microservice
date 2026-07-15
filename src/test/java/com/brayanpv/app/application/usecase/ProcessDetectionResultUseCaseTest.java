package com.brayanpv.app.application.usecase;

import com.brayanpv.app.domain.messaging.IEventPublisherPort;
import com.brayanpv.app.domain.messaging.IImageEventResultBroker;
import com.brayanpv.app.domain.model.BirdDetectionResult;
import com.brayanpv.app.domain.model.ClassificationRequested;
import com.brayanpv.app.domain.model.ImageEvent;
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
class ProcessDetectionResultUseCaseTest {

    private static final String CLASSIFICATION_ROUTING_KEY = "bird_classification.pending";

    @Mock
    private IImageEventRepository imageEventRepository;

    @Mock
    private IEventPublisherPort eventPublisherPort;

    @Mock
    private IImageEventResultBroker resultBroker;

    private ProcessDetectionResultUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessDetectionResultUseCase(imageEventRepository, eventPublisherPort, resultBroker);
        ReflectionTestUtils.setField(useCase, "classificationRoutingKey", CLASSIFICATION_ROUTING_KEY);
    }

    @Test
    void execute_whenIsBird_publishesClassificationRequestedAndDoesNotCompleteBroker() {
        UUID imageEventId = UUID.randomUUID();
        BirdDetectionResult result = new BirdDetectionResult();
        result.setImageEventId(imageEventId);
        result.setBird(true);
        result.setConfidence(BigDecimal.valueOf(0.9));

        ImageEvent updated = ImageEvent.builder()
                .id(imageEventId)
                .s3Key("images/key.jpg")
                .status(ImageStatus.BIRD_DETECTED)
                .build();
        when(imageEventRepository.updateDetection(imageEventId, true, result.getConfidence()))
                .thenReturn(Mono.just(updated));
        when(eventPublisherPort.publish(eq(CLASSIFICATION_ROUTING_KEY), any(ClassificationRequested.class)))
                .thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(result)).verifyComplete();

        ArgumentCaptor<ClassificationRequested> captor = ArgumentCaptor.forClass(ClassificationRequested.class);
        verify(eventPublisherPort).publish(eq(CLASSIFICATION_ROUTING_KEY), captor.capture());
        assertThat(captor.getValue().getImageEventId()).isEqualTo(imageEventId);
        assertThat(captor.getValue().getS3Key()).isEqualTo("images/key.jpg");
        verify(resultBroker, never()).complete(any());
    }

    @Test
    void execute_whenNotBird_completesBrokerAndDoesNotPublish() {
        UUID imageEventId = UUID.randomUUID();
        BirdDetectionResult result = new BirdDetectionResult();
        result.setImageEventId(imageEventId);
        result.setBird(false);
        result.setConfidence(BigDecimal.valueOf(0.2));

        ImageEvent updated = ImageEvent.builder()
                .id(imageEventId)
                .status(ImageStatus.NOT_A_BIRD)
                .build();
        when(imageEventRepository.updateDetection(imageEventId, false, result.getConfidence()))
                .thenReturn(Mono.just(updated));

        StepVerifier.create(useCase.execute(result)).verifyComplete();

        verify(resultBroker).complete(updated);
        verify(eventPublisherPort, never()).publish(any(), any());
    }

    @Test
    void execute_whenRepositoryFails_propagatesError() {
        UUID imageEventId = UUID.randomUUID();
        BirdDetectionResult result = new BirdDetectionResult();
        result.setImageEventId(imageEventId);
        result.setBird(true);

        when(imageEventRepository.updateDetection(eq(imageEventId), eq(true), any()))
                .thenReturn(Mono.error(new RuntimeException("db down")));

        StepVerifier.create(useCase.execute(result))
                .expectErrorMessage("db down")
                .verify();
    }
}
