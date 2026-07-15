package com.brayanpv.app.application.usecase;

import com.brayanpv.app.application.dto.response.ImageStatusResponse;
import com.brayanpv.app.domain.usecase.contracts.IGetImageStatusUseCase;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class GetImageStatusUseCase implements IGetImageStatusUseCase {

    private final IImageEventRepository imageEventRepository;

    @Override
    public Mono<ImageStatusResponse> execute(UUID imageEventId) {
        log.info("Getting image status for {}", imageEventId);
        return imageEventRepository.findById(imageEventId)
                .map(imageEvent -> ImageStatusResponse.builder()
                        .imageEventId(imageEvent.getId())
                        .specieConfidence(imageEvent.getSpecieConfidence())
                        .status(imageEvent.getStatus())
                        .failureReason(imageEvent.getFailureReason())
                        .build());
    }
}
