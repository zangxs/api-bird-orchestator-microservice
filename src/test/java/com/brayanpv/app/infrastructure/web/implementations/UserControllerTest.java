package com.brayanpv.app.infrastructure.web.implementations;

import com.brayanpv.app.application.dto.response.BirdSightingResponse;
import com.brayanpv.app.application.dto.response.GenericResponse;
import com.brayanpv.app.domain.usecase.contracts.IGetImageEventUseCase;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.storage.IImageStoragePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private IGetImageEventUseCase getImageEventUseCase;

    @Mock
    private IImageStoragePort imageStoragePort;

    @InjectMocks
    private UserController userController;

    @Test
    void getImageEvents_returnsWrappedListWithThumbnailUrls() {
        UUID userId = UUID.randomUUID();
        ImageEvent imageEvent = ImageEvent.builder()
                .id(UUID.randomUUID())
                .s3Key("images/key.jpg")
                .status(ImageStatus.DONE)
                .scientificName("Turdus merula")
                .build();

        when(getImageEventUseCase.getImageEvents(userId)).thenReturn(Flux.just(imageEvent));
        when(imageStoragePort.generatePresignedUrl(eq("images/key.jpg"), any(Duration.class)))
                .thenReturn(Mono.just("https://s3/presigned-url"));

        StepVerifier.create(userController.getImageEvents(userId))
                .assertNext(response -> {
                    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                    GenericResponse<List<BirdSightingResponse>> body = response.getBody();
                    assertThat(body).isNotNull();
                    assertThat(body.getData()).hasSize(1);
                    BirdSightingResponse sighting = body.getData().get(0);
                    assertThat(sighting.getImageEventId()).isEqualTo(imageEvent.getId());
                    assertThat(sighting.getThumbnailUrl()).isEqualTo("https://s3/presigned-url");
                })
                .verifyComplete();
    }

    @Test
    void getImageEvents_whenUserHasNoSightings_returnsEmptyList() {
        UUID userId = UUID.randomUUID();
        when(getImageEventUseCase.getImageEvents(userId)).thenReturn(Flux.empty());

        StepVerifier.create(userController.getImageEvents(userId))
                .assertNext(response -> assertThat(response.getBody().getData()).isEmpty())
                .verifyComplete();
    }
}
