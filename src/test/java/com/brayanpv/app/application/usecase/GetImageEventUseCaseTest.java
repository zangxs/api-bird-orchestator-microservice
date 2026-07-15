package com.brayanpv.app.application.usecase;

import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetImageEventUseCaseTest {

    @Mock
    private IImageEventRepository imageEventRepository;

    private GetImageEventUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetImageEventUseCase(imageEventRepository);
    }

    @Test
    void getImageEvents_delegatesToRepositoryFindByUserId() {
        UUID userId = UUID.randomUUID();
        ImageEvent event = ImageEvent.builder().id(UUID.randomUUID()).userId(userId).status(ImageStatus.DONE).build();
        when(imageEventRepository.findByUserId(userId)).thenReturn(Flux.just(event));

        StepVerifier.create(useCase.getImageEvents(userId))
                .expectNext(event)
                .verifyComplete();
    }

    @Test
    void getImageEvents_whenUserHasNoSightings_returnsEmptyFlux() {
        UUID userId = UUID.randomUUID();
        when(imageEventRepository.findByUserId(userId)).thenReturn(Flux.empty());

        StepVerifier.create(useCase.getImageEvents(userId)).verifyComplete();
    }
}
