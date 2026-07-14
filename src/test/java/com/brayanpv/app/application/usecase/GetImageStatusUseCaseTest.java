package com.brayanpv.app.application.usecase;

import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetImageStatusUseCaseTest {

    @Mock
    private IImageEventRepository imageEventRepository;

    private GetImageStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetImageStatusUseCase(imageEventRepository);
    }

    @Test
    void execute_returnsMappedStatus() {
        UUID imageEventId = UUID.randomUUID();
        ImageEvent imageEvent = ImageEvent.builder()
                .id(imageEventId)
                .status(ImageStatus.DONE)
                .specieConfidence(BigDecimal.valueOf(0.95))
                .build();

        when(imageEventRepository.findById(imageEventId)).thenReturn(Mono.just(imageEvent));

        StepVerifier.create(useCase.execute(imageEventId))
                .assertNext(response -> {
                    assertThat(response.getImageEventId()).isEqualTo(imageEventId);
                    assertThat(response.getImageStatus()).isEqualTo(ImageStatus.DONE);
                    assertThat(response.getSpecieConfidence()).isEqualByComparingTo(BigDecimal.valueOf(0.95));
                })
                .verifyComplete();
    }

    @Test
    void execute_whenNotFound_propagatesError() {
        UUID imageEventId = UUID.randomUUID();
        when(imageEventRepository.findById(imageEventId))
                .thenReturn(Mono.error(new com.brayanpv.app.domain.exception.ImageEventNotFoundException("not found")));

        StepVerifier.create(useCase.execute(imageEventId))
                .expectError(com.brayanpv.app.domain.exception.ImageEventNotFoundException.class)
                .verify();
    }
}
