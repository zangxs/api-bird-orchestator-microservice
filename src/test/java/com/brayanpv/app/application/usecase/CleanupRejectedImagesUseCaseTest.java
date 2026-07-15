package com.brayanpv.app.application.usecase;

import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.domain.storage.IImageStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CleanupRejectedImagesUseCaseTest {

    @Mock
    private IImageEventRepository imageEventRepository;

    @Mock
    private IImageStoragePort imageStoragePort;

    private CleanupRejectedImagesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CleanupRejectedImagesUseCase(imageEventRepository, imageStoragePort);
    }

    @Test
    void execute_deletesS3ObjectAndMarksExpiredForEachRejectedEvent() {
        ImageEvent event1 = ImageEvent.builder().id(UUID.randomUUID()).s3Key("images/a.jpg").status(ImageStatus.NOT_A_BIRD).build();
        ImageEvent event2 = ImageEvent.builder().id(UUID.randomUUID()).s3Key("images/b.jpg").status(ImageStatus.NOT_A_BIRD).build();

        when(imageEventRepository.findByStatus(ImageStatus.NOT_A_BIRD)).thenReturn(Flux.just(event1, event2));
        when(imageStoragePort.delete(event1.getS3Key())).thenReturn(Mono.empty());
        when(imageStoragePort.delete(event2.getS3Key())).thenReturn(Mono.empty());
        when(imageEventRepository.markExpired(event1.getId())).thenReturn(Mono.just(event1));
        when(imageEventRepository.markExpired(event2.getId())).thenReturn(Mono.just(event2));

        StepVerifier.create(useCase.execute()).verifyComplete();

        verify(imageStoragePort).delete(event1.getS3Key());
        verify(imageStoragePort).delete(event2.getS3Key());
        verify(imageEventRepository).markExpired(event1.getId());
        verify(imageEventRepository).markExpired(event2.getId());
    }

    @Test
    void execute_whenOneEventFailsToClean_stillProcessesTheRest() {
        ImageEvent failing = ImageEvent.builder().id(UUID.randomUUID()).s3Key("images/broken.jpg").status(ImageStatus.NOT_A_BIRD).build();
        ImageEvent healthy = ImageEvent.builder().id(UUID.randomUUID()).s3Key("images/ok.jpg").status(ImageStatus.NOT_A_BIRD).build();

        when(imageEventRepository.findByStatus(ImageStatus.NOT_A_BIRD)).thenReturn(Flux.just(failing, healthy));
        when(imageStoragePort.delete(failing.getS3Key())).thenReturn(Mono.error(new RuntimeException("s3 down")));
        when(imageStoragePort.delete(healthy.getS3Key())).thenReturn(Mono.empty());
        // markExpired(failing.getId()) is constructed eagerly as a .then(...) argument even though
        // it's never subscribed (delete() already errored) - stub it to satisfy strict-stub checks.
        when(imageEventRepository.markExpired(failing.getId())).thenReturn(Mono.empty());
        when(imageEventRepository.markExpired(healthy.getId())).thenReturn(Mono.just(healthy));

        StepVerifier.create(useCase.execute()).verifyComplete();

        verify(imageEventRepository).markExpired(healthy.getId());
    }

    @Test
    void execute_whenNoRejectedEvents_completesWithoutSideEffects() {
        when(imageEventRepository.findByStatus(ImageStatus.NOT_A_BIRD)).thenReturn(Flux.empty());

        StepVerifier.create(useCase.execute()).verifyComplete();
    }
}
