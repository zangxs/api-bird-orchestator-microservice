package com.brayanpv.app.router;

import com.brayanpv.app.handler.ExampleHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;

@Configuration
public class ExampleRouter {

    @Bean
    public RouterFunction<ServerResponse> routes(ExampleHandler handler) {
        return RouterFunctions.route()
                .GET("/api/example", handler::getAll)
                .GET("/api/example/{id}", handler::getById)
                .POST("/api/example", handler::create)
                .DELETE("/api/example/{id}", handler::delete)
                .build();
    }

}
