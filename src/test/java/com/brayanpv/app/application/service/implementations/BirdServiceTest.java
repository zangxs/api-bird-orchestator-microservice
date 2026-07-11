package com.brayanpv.app.application.service.implementations;

import com.brayanpv.app.application.dto.request.ImageUploadRequest;
import com.brayanpv.app.domain.messaging.IEventPublisherPort;
import com.brayanpv.app.domain.model.BirdObserved;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.domain.storage.IImageStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BirdServiceTest {

    private static final String ROUTING_KEY = "bird_detection.pending";

    @Mock
    private IImageStoragePort imageStoragePort;

    @Mock
    private IEventPublisherPort eventPublisherPort;

    @Mock
    private IImageEventRepository imageEventRepository;

    private BirdService birdService;

    @BeforeEach
    void setUp() {
        birdService = new BirdService(imageStoragePort, eventPublisherPort, imageEventRepository);
        ReflectionTestUtils.setField(birdService, "routingKey", ROUTING_KEY);
    }

    private FilePart mockFilePart(byte[] content) {
        FilePart filePart = mock(FilePart.class);
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(content);
        when(filePart.content()).thenReturn(Flux.just(buffer));
        return filePart;
    }

    @Test
    void processImage_happyPath_uploadsSavesAndPublishes() {
        UUID userId = UUID.randomUUID();
        UUID imageEventId = UUID.randomUUID();
        FilePart filePart = mockFilePart("fake-image-bytes".getBytes(StandardCharsets.UTF_8));
        ImageUploadRequest request = new ImageUploadRequest(filePart, userId);

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

        StepVerifier.create(birdService.processImage(request))
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
    void processImage_whenPublishFails_propagatesError() {
        UUID userId = UUID.randomUUID();
        UUID imageEventId = UUID.randomUUID();
        FilePart filePart = mockFilePart("fake-image-bytes".getBytes(StandardCharsets.UTF_8));
        ImageUploadRequest request = new ImageUploadRequest(filePart, userId);

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

        StepVerifier.create(birdService.processImage(request))
                .expectErrorMessage("publish failed")
                .verify();
    }
}
