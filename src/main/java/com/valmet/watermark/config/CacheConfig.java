package com.valmet.watermark.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.time.Duration;

/**
 * Configuration class for setting up Redis-based caching in the application.
 * <p>
 * Enables Spring's annotation-driven cache management capability and customizes
 * the cache manager and error handling for Redis cache operations.
 */
@Slf4j
@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {
    /**
     * Time-to-live (TTL) for cache entries in milliseconds, injected from
     * application properties.
     */
    @Value("${spring.cache.redis.time-to-live}")
    private long ttl;

    /**
     * Configures the {@link CacheManager} bean to use Redis with a specified TTL
     * and disables caching of null values.
     *
     * @param redisConnectionFactory the Redis connection factory
     * @return a configured {@link CacheManager} instance
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
	RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofMillis(ttl))
		.disableCachingNullValues();
	return RedisCacheManager.builder(redisConnectionFactory).cacheDefaults(config).build();
    }

    /**
     * Provides a custom {@link CacheErrorHandler} to log cache operation errors.
     *
     * @return a custom error handler for cache operations
     */
    @Override
    public CacheErrorHandler errorHandler() {
	return new SimpleCacheErrorHandler();
    }

    /**
     * Custom implementation of {@link CacheErrorHandler} that logs cache errors.
     */
    private static class SimpleCacheErrorHandler implements CacheErrorHandler {
	@Override
	public void handleCacheGetError(RuntimeException e, org.springframework.cache.Cache cache, Object key) {
	    log.error("Cache get error on key: {} , Error: {}", key, e.getMessage());
	}

	@Override
	public void handleCachePutError(RuntimeException e, org.springframework.cache.Cache cache, Object key,
		Object value) {
	    log.error("Cache put error on key: {} , Value: {}, Error: {}", key, value, e.getMessage());
	}

	@Override
	public void handleCacheEvictError(RuntimeException e, org.springframework.cache.Cache cache, Object key) {
	    log.error("Cache evict error on key: {} , Error: {}", key, e.getMessage());
	}

	@Override
	public void handleCacheClearError(RuntimeException e, org.springframework.cache.Cache cache) {
	    log.error("Cache clear error, Error: {}", e.getMessage());
	}
    }
}