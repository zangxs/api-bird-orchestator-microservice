package com.brayanpv.app.application.usecase;

import com.brayanpv.app.application.dto.request.ImageUploadRequest;
import com.brayanpv.app.domain.messaging.IEventPublisherPort;
import com.brayanpv.app.domain.messaging.IImageEventResultBroker;
import com.brayanpv.app.domain.model.BirdObserved;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.domain.storage.IImageStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProcessBirdImageUseCaseTest {

    private static final String ROUTING_KEY = "bird_detection.pending";

    @Mock
    private IImageStoragePort imageStoragePort;

    @Mock
    private IEventPublisherPort eventPublisherPort;

    @Mock
    private IImageEventRepository imageEventRepository;

    @Mock
    private IImageEventResultBroker resultBroker;

    private ProcessBirdImageUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ProcessBirdImageUseCase(imageStoragePort, eventPublisherPort, imageEventRepository, resultBroker);
        ReflectionTestUtils.setField(useCase, "routingKey", ROUTING_KEY);
    }

    private FilePart mockFilePart(byte[] content) {
        FilePart filePart = mock(FilePart.class);
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(content);
        when(filePart.content()).thenReturn(Flux.just(buffer));
        return filePart;
    }

    @Test
    void execute_whenUploadExceedsMaxSize_rejectsWithoutBufferingUnboundedMemory() {
        UUID userId = UUID.randomUUID();
        // One buffer just over the 10 MB cap declared in ProcessBirdImageUseCase.
        byte[] oversized = new byte[10 * 1024 * 1024 + 1];
        FilePart filePart = mockFilePart(oversized);
        ImageUploadRequest request = new ImageUploadRequest(filePart, userId, null, null);

        // .then(saveImageEvent(...)) constructs its argument mono eagerly regardless of whether
        // the upstream chain ever reaches it, so imageEventRepository.save(...) must be stubbed
        // even though this path always errors out before extractBytes() completes.
        when(imageEventRepository.save(any(ImageEvent.class))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(request))
                .expectError(IllegalStateException.class) // DataBufferLimitException
                .verify();
    }

    @Test
    void execute_happyPath_uploadsSavesPublishesAndAwaitsResult() {
        UUID userId = UUID.randomUUID();
        UUID imageEventId = UUID.randomUUID();
        FilePart filePart = mockFilePart("fake-image-bytes".getBytes(StandardCharsets.UTF_8));
        ImageUploadRequest request = new ImageUploadRequest(filePart, userId, null, null);

        when(imageStoragePort.upload(any(byte[].class), anyString())).thenReturn(Mono.just("s3-key"));

        ArgumentCaptor<ImageEvent> savedEventCaptor = ArgumentCaptor.forClass(ImageEvent.class);
        when(imageEventRepository.save(savedEventCaptor.capture())).thenAnswer(invocation -> {
            ImageEvent toSave = invocation.getArgument(0);
            ImageEvent saved = ImageEvent.builder()
                    .id(imageEventId)
                    .userId(toSave.getUserId())
                    .s3Key(toSave.getS3Key())
                    .status(toSave.getStatus())
                    .build();
            return Mono.just(saved);
        });

        when(eventPublisherPort.publish(eq(ROUTING_KEY), any(BirdObserved.class))).thenReturn(Mono.empty());
        when(resultBroker.awaitResult(eq(imageEventId), any(Duration.class))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(request))
                .assertNext(response -> {
                    assertThat(response.getImageEventId()).isEqualTo(imageEventId);
                    assertThat(response.getStatus()).isEqualTo(ImageStatus.PROCESSING.name());
                })
                .verifyComplete();

        ImageEvent savedEvent = savedEventCaptor.getValue();
        assertThat(savedEvent.getUserId()).isEqualTo(userId);
        assertThat(savedEvent.getStatus()).isEqualTo(ImageStatus.PROCESSING);
        assertThat(savedEvent.getS3Key()).startsWith("images/" + userId + "/");

        verify(imageStoragePort).upload(any(byte[].class), eq(savedEvent.getS3Key()));

        ArgumentCaptor<BirdObserved> eventCaptor = ArgumentCaptor.forClass(BirdObserved.class);
        verify(eventPublisherPort).publish(eq(ROUTING_KEY), eventCaptor.capture());
        BirdObserved published = eventCaptor.getValue();
        assertThat(published.getImageEventId()).isEqualTo(imageEventId);
        assertThat(published.getUserId()).isEqualTo(userId);
        assertThat(published.getS3Key()).isEqualTo(savedEvent.getS3Key());
        assertThat(published.getStatus()).isEqualTo(ImageStatus.PROCESSING.name());
    }

    @Test
    void execute_withCoordinates_savesLatitudeAndLongitudeOnImageEvent() {
        UUID userId = UUID.randomUUID();
        UUID imageEventId = UUID.randomUUID();
        BigDecimal latitude = new BigDecimal("40.712800");
        BigDecimal longitude = new BigDecimal("-74.006000");
        FilePart filePart = mockFilePart("fake-image-bytes".getBytes(StandardCharsets.UTF_8));
        ImageUploadRequest request = new ImageUploadRequest(filePart, userId, longitude, latitude);

        when(imageStoragePort.upload(any(byte[].class), anyString())).thenReturn(Mono.just("s3-key"));

        ArgumentCaptor<ImageEvent> savedEventCaptor = ArgumentCaptor.forClass(ImageEvent.class);
        when(imageEventRepository.save(savedEventCaptor.capture())).thenAnswer(invocation -> {
            ImageEvent toSave = invocation.getArgument(0);
            return Mono.just(ImageEvent.builder()
                    .id(imageEventId)
                    .userId(toSave.getUserId())
                    .s3Key(toSave.getS3Key())
                    .status(toSave.getStatus())
                    .latitude(toSave.getLatitude())
                    .longitude(toSave.getLongitude())
                    .build());
        });
        when(eventPublisherPort.publish(anyString(), any())).thenReturn(Mono.empty());
        when(resultBroker.awaitResult(eq(imageEventId), any(Duration.class))).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(request))
                .assertNext(response -> assertThat(response.getImageEventId()).isEqualTo(imageEventId))
                .verifyComplete();

        ImageEvent savedEvent = savedEventCaptor.getValue();
        assertThat(savedEvent.getLatitude()).isEqualByComparingTo(latitude);
        assertThat(savedEvent.getLongitude()).isEqualByComparingTo(longitude);
    }

    @Test
    void execute_whenBrokerResolvesResult_returnsResolvedStatus() {
        UUID userId = UUID.randomUUID();
        UUID imageEventId = UUID.randomUUID();
        FilePart filePart = mockFilePart("fake-image-bytes".getBytes(StandardCharsets.UTF_8));
        ImageUploadRequest request = new ImageUploadRequest(filePart, userId, null, null);

        when(imageStoragePort.upload(any(byte[].class), anyString())).thenReturn(Mono.just("s3-key"));
        when(imageEventRepository.save(any(ImageEvent.class))).thenAnswer(invocation -> {
            ImageEvent toSave = invocation.getArgument(0);
            return Mono.just(ImageEvent.builder()
                    .id(imageEventId)
                    .userId(toSave.getUserId())
                    .s3Key(toSave.getS3Key())
                    .status(toSave.getStatus())
                    .build());
        });
        when(eventPublisherPort.publish(anyString(), any())).thenReturn(Mono.empty());

        ImageEvent resolved = ImageEvent.builder()
                .id(imageEventId)
                .userId(userId)
                .status(ImageStatus.BIRD_DETECTED)
                .build();
        when(resultBroker.awaitResult(eq(imageEventId), any(Duration.class))).thenReturn(Mono.just(resolved));

        StepVerifier.create(useCase.execute(request))
                .assertNext(response -> assertThat(response.getStatus()).isEqualTo(ImageStatus.BIRD_DETECTED.name()))
                .verifyComplete();
    }

    @Test
    void execute_whenPublishFails_propagatesError() {
        UUID userId = UUID.randomUUID();
        UUID imageEventId = UUID.randomUUID();
        FilePart filePart = mockFilePart("fake-image-bytes".getBytes(StandardCharsets.UTF_8));
        ImageUploadRequest request = new ImageUploadRequest(filePart, userId, null, null);

        when(imageStoragePort.upload(any(byte[].class), anyString())).thenReturn(Mono.just("s3-key"));
        when(imageEventRepository.save(any(ImageEvent.class))).thenAnswer(invocation -> {
            ImageEvent toSave = invocation.getArgument(0);
            return Mono.just(ImageEvent.builder()
                    .id(imageEventId)
                    .userId(toSave.getUserId())
                    .s3Key(toSave.getS3Key())
                    .status(toSave.getStatus())
                    .build());
        });
        when(eventPublisherPort.publish(anyString(), any())).thenReturn(Mono.error(new RuntimeException("publish failed")));

        StepVerifier.create(useCase.execute(request))
                .expectErrorMessage("publish failed")
                .verify();
    }
}
