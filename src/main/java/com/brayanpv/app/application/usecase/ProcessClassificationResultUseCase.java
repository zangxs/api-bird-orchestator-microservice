package com.brayanpv.app.application.usecase;

import com.brayanpv.app.domain.model.BirdClassificationResult;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.domain.usecase.contracts.IProcessClassificationResultUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Log4j2
public class ProcessClassificationResultUseCase implements IProcessClassificationResultUseCase {

    private final IImageEventRepository imageEventRepository;

    @Override
    public Mono<Void> execute(BirdClassificationResult result) {
        log.info("Process Classification Result");
        return imageEventRepository.updateClassification(
                result.getImageEventId(),
                result.getSpecie(),
                result.getSpecieConfidence(),
                result.getFailureReason()
        ).then(); //aca despues de que hayamos clasificado debe mandar a una cola para clasificacion manual ya que el modelo nos dijo que era un ave, pero no esta entrenado para ella
    }

}
