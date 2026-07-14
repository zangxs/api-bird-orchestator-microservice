package com.brayanpv.app.application.usecase;

import com.brayanpv.app.application.usecase.contracts.IGetImageEventUseCase;
import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Log4j2
public class GetImageEventUseCase implements IGetImageEventUseCase {

    private final IImageEventRepository imageEventRepository;

    @Override
    public Flux<ImageEvent> getImageEvents(UUID userId) {
        return imageEventRepository.findByUserId(userId);
    }
}
