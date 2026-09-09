package com.example.spring_boot_project_api.service.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

/**
 * Lightweight in-memory rate limiter. Keys are typically "user:email:purpose"
 * for per-account OTP throttling and "ip" for per-IP limits. This is a
 * single-instance limiter; for a multi-node deployment it should be replaced
 * with a shared store (Redis etc.), but the API contract stays the same.
 */
@Service
public class RateLimiterService {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Returns {@code true} if the key is allowed (i.e. request may proceed) and
     * records the request; {@code false} if the limit has been exceeded.
     */
    public boolean tryAcquire(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        Bucket bucket = buckets.compute(key, (k, b) -> {
            if (b == null || b.windowStart.plus(window).isBefore(now)) {
                return new Bucket(now, 1);
            }
            b.count++;
            return b;
        });
        return bucket.count <= maxRequests;
    }

    public boolean tryAcquire(String key, int maxRequests) {
        return tryAcquire(key, maxRequests, Duration.ofMinutes(15));
    }

    private static final class Bucket {
        final Instant windowStart;
        int count;

        Bucket(Instant windowStart, int count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
