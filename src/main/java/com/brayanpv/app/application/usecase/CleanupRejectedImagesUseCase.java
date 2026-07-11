package com.brayanpv.app.application.usecase;

import com.brayanpv.app.domain.model.enums.ImageStatus;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.domain.storage.IImageStoragePort;
import com.brayanpv.app.domain.usecase.contracts.ICleanupRejectedImagesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Log4j2
public class CleanupRejectedImagesUseCase implements ICleanupRejectedImagesUseCase {

    private final IImageEventRepository imageEventRepository;
    private final IImageStoragePort imageStoragePort;

    @Override
    public Mono<Void> execute() {
        return imageEventRepository.findByStatus(ImageStatus.NOT_A_BIRD)
                .flatMap(imageEvent -> imageStoragePort.delete(imageEvent.getS3Key())
                        .then(imageEventRepository.markExpired(imageEvent.getId()))
                        .doOnSuccess(v -> log.info("Cleaned up rejected image event {}", imageEvent.getId()))
                        .onErrorResume(e -> {
                            log.error("Failed to clean up rejected image event {}", imageEvent.getId(), e);
                            return Mono.empty();
                        }))
                .then();
    }
}
