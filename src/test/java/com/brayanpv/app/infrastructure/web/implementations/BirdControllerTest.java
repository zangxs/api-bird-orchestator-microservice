package com.brayanpv.app.infrastructure.web.implementations;

import com.brayanpv.app.application.dto.response.GenericResponse;
import com.brayanpv.app.application.dto.response.ImageUploadResponse;
import com.brayanpv.app.application.usecase.contracts.IGetImageStatusUseCase;
import com.brayanpv.app.application.usecase.contracts.IProcessBirdImageUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BirdControllerTest {

    @Mock
    private IProcessBirdImageUseCase processBirdImageUseCase;

    @Mock
    private IGetImageStatusUseCase getImageStatusUseCase;

    @InjectMocks
    private BirdController birdController;

    @Test
    void detectBird_whenUseCaseReturnsSuccess_returnsWrappedResponse() {
        // Given
        UUID imageEventId = UUID.randomUUID();
        ImageUploadResponse useCaseResponse = ImageUploadResponse.builder()
                .imageEventId(imageEventId)
                .status("PROCESSING")
                .build();

        when(processBirdImageUseCase.execute(any())).thenReturn(Mono.just(useCaseResponse));

        // When
        Mono<ResponseEntity<GenericResponse<ImageUploadResponse>>> result =
                birdController.detectBird(null, String.valueOf(UUID.randomUUID()));

        // Then - verify controller wraps use case response correctly
        ResponseEntity<GenericResponse<ImageUploadResponse>> response = result.block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        GenericResponse<ImageUploadResponse> body = response.getBody();
        assertThat(body.getCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(body.getData()).isNotNull();

        ImageUploadResponse data = body.getData();
        assertThat(data.getImageEventId()).isEqualTo(imageEventId);
        assertThat(data.getStatus()).isEqualTo("PROCESSING");
        assertThat(body.getDateTime()).isNotNull();

        // Verify use case was called
        org.mockito.Mockito.verify(processBirdImageUseCase).execute(any());
    }

    @Test
    void detectBird_whenUseCaseThrowsError_propagatesError() {
        // Given
        when(processBirdImageUseCase.execute(any())).thenReturn(Mono.error(new RuntimeException("S3 upload failed")));

        // When/Then - error propagates to GlobalExceptionHandler
        Mono<ResponseEntity<GenericResponse<ImageUploadResponse>>> result =
                birdController.detectBird(null, String.valueOf(UUID.randomUUID()));

        try {
            result.block();
        } catch (Exception e) {
            // Error propagates - just verify it's a RuntimeException
            assertThat(e).isInstanceOf(RuntimeException.class);
            assertThat(e.getMessage()).isEqualTo("S3 upload failed");
        }

        org.mockito.Mockito.verify(processBirdImageUseCase).execute(any());
    }

    @Test
    void detectBird_controllerIsWiredCorrectly() {
        // Verify controller is instantiated with mocked use cases
        assertThat(birdController).isNotNull();
        assertThat(processBirdImageUseCase).isNotNull();
        assertThat(getImageStatusUseCase).isNotNull();
    }
}
