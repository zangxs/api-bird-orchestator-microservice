package com.brayanpv.app.infrastructure.scheduling;

import com.brayanpv.app.domain.usecase.contracts.ICleanupRejectedImagesUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Log4j2
public class RejectedImageCleanupScheduler {

    private final ICleanupRejectedImagesUseCase cleanupRejectedImagesUseCase;

    @Scheduled(fixedDelayString = "${cleanup.rejected-images.fixed-delay-ms:3600000}")
    public void run() {
        cleanupRejectedImagesUseCase.execute()
                .doOnError(e -> log.error("Rejected image cleanup run failed", e))
                .subscribe();
    }
}
