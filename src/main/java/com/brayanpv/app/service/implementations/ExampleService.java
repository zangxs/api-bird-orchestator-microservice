package com.brayanpv.app.service.implementations;
import com.brayanpv.app.service.contracts.IExampleService;
import com.brayanpv.app.model.response.ExampleResponse;
import reactor.core.publisher.Mono;

public class ExampleService implements IExampleService {
    @Override
    public Mono<ExampleResponse> getExample() {
        return null;
    }
}
