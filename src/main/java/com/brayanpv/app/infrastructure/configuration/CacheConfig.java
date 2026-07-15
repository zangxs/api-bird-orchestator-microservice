package com.brayanpv.app.infrastructure.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Each cache gets its own Caffeine spec (size/TTL), so this builds the {@link CacheManager}
 * explicitly instead of relying on Spring Boot's single-spec Caffeine autoconfiguration
 * ({@code spring.cache.caffeine.spec} applies one spec to every cache name).
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${cache.species.spec}") String speciesSpec,
            @Value("${cache.presigned-url.spec}") String presignedUrlSpec) {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                new CaffeineCache("speciesByScientificName", Caffeine.from(speciesSpec).build()),
                new CaffeineCache("speciesById", Caffeine.from(speciesSpec).build()),
                new CaffeineCache("presignedImageUrls", Caffeine.from(presignedUrlSpec).build())
        ));
        return cacheManager;
    }
}
