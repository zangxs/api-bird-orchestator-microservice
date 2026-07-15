package com.brayanpv.app.infrastructure.configuration;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Each cache gets its own Caffeine spec (size/TTL), so this builds the {@link CacheManager}
 * explicitly instead of relying on Spring Boot's single-spec Caffeine autoconfiguration
 * ({@code spring.cache.caffeine.spec} applies one spec to every cache name).
 * <p>
 * {@code @Cacheable} methods here return {@code Mono}/{@code Flux} (e.g. {@code SpecieCacheService}),
 * which Spring's caching abstraction bridges via {@link org.springframework.cache.Cache#retrieve} —
 * that requires an <em>async</em> Caffeine cache, hence {@code setAsyncCacheMode(true)} and
 * {@code buildAsync()} below rather than plain sync {@code CaffeineCache}s.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(
            @Value("${cache.species.spec}") String speciesSpec,
            @Value("${cache.presigned-url.spec}") String presignedUrlSpec) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setAsyncCacheMode(true);
        cacheManager.registerCustomCache("speciesByScientificName", Caffeine.from(speciesSpec).buildAsync());
        cacheManager.registerCustomCache("speciesById", Caffeine.from(speciesSpec).buildAsync());
        cacheManager.registerCustomCache("presignedImageUrls", Caffeine.from(presignedUrlSpec).buildAsync());
        return cacheManager;
    }
}
