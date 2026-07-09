package com.brayanpv.app.handler;

import com.brayanpv.app.model.ExampleModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class ExampleHandler {

    public Mono<ServerResponse> getAll(ServerRequest request) {
        return ServerResponse.ok().bodyValue("GET all - implementar");
    }

    public Mono<ServerResponse> getById(ServerRequest request) {
        String id = request.pathVariable("id");
        return ServerResponse.ok().bodyValue("GET by id: " + id + " - implementar");
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(ExampleModel.class)
                .flatMap(body -> ServerResponse.ok().bodyValue("POST create - implementar"));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        String id = request.pathVariable("id");
        return ServerResponse.noContent().build();
    }

}
