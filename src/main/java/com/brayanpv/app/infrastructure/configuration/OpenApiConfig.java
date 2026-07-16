package com.brayanpv.app.infrastructure.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI birdDexOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bird-Dex Orchestrator API")
                        .description("""
                                Entry point of the bird-dex pipeline: uploads a bird photo, tracks its \
                                detection/classification lifecycle through Postgres, and serves sighting \
                                and map data. POST /bird/detect holds the HTTP response open (up to 6s) \
                                while the async RabbitMQ pipeline (detection -> classification) resolves.""")
                        .version("v1")
                        .contact(new Contact().name("bird-dex")));
    }
}
