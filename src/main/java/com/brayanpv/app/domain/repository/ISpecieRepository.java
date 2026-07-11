package com.brayanpv.app.domain.repository;

import com.brayanpv.app.domain.model.Specie;
import reactor.core.publisher.Mono;

public interface ISpecieRepository {
    Mono<Specie> findByScientificName(String scientificName);

}
