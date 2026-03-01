package com.bytehealers.healverse.config;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configure cache manager with Caffeine for high-performance caching
     * nutritionalContext cache: Stores user's eating pattern analysis
     * - Expires after 24 hours (refreshed daily)
     * - Maximum 1000 entries
     * - Improves performance by avoiding repeated database queries
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("nutritionalContext");
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * Caffeine cache configuration
     * - expireAfterWrite: Cache expires 24 hours after being written
     * - maximumSize: Maximum 1000 cached entries to prevent memory issues
     * - recordStats: Enable statistics for monitoring cache performance
     */
    private Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .expireAfterWrite(24, TimeUnit.HOURS)  // Cache for 24 hours
                .maximumSize(1000)                      // Max 1000 users' context cached
                .recordStats();                         // Enable cache statistics
    }
}