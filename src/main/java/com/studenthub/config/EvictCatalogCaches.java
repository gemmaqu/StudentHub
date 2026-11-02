package com.studenthub.config;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cached course responses include a resource count and cached resource responses include course
 * details, so a write to either entity invalidates both caches.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Caching(evict = {
        @CacheEvict(value = "courses", allEntries = true),
        @CacheEvict(value = "resources", allEntries = true)
})
public @interface EvictCatalogCaches {
}
