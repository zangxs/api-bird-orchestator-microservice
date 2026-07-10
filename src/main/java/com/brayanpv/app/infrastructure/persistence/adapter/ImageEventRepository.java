package com.brayanpv.app.infrastructure.persistence.adapter;

import com.brayanpv.app.domain.model.ImageEvent;
import com.brayanpv.app.domain.repository.IImageEventRepository;
import com.brayanpv.app.infrastructure.mapper.ImageEventMapper;
import com.brayanpv.app.infrastructure.persistence.entity.ImageEventEntity;
import com.brayanpv.app.infrastructure.persistence.repository.IImageEventR2DBCRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class ImageEventRepository implements IImageEventRepository {

    private final IImageEventR2DBCRepository imageEventR2DBCRepository;
    private final ImageEventMapper imageEventMapper;

    @Override
    public Mono<ImageEvent> save(ImageEvent imageEvent) {

        return Mono.just(imageEvent)
                .flatMap(image -> Mono.just(imageEventMapper.toEntityFromModel(image)))
                .flatMap(imageEventR2DBCRepository::save)
                .flatMap(saved -> Mono.just(imageEventMapper.toModelFromEntity(saved)));

    }
}
