package com.brayanpv.app.service.contracts;

import com.brayanpv.app.model.response.ExampleResponse;
import reactor.core.publisher.Mono;

public interface IExampleService {

    Mono<ExampleResponse> getExample();
}