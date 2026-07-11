package com.brayanpv.app.infrastructure.persistence.adapter;

import com.brayanpv.app.domain.model.Specie;
import com.brayanpv.app.domain.repository.ISpecieRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class SpecieRepository implements ISpecieRepository {

    @Override
    public Mono<Specie> findByScientificName(String scientificName) {
        return null;
    }
}
