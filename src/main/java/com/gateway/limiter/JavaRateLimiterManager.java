package com.gateway.limiter;

import java.util.concurrent.ConcurrentHashMap;

public class JavaRateLimiterManager implements RateLimiter {

    // Inner class representing an individual Tenant's bucket
    private static class TokenBucket {
        private final int capacity;
        private final double refillRatePerSec;
        private double tokens;
        private long lastUpdate;

        TokenBucket(int capacity, double refillRatePerSec) {
            this.capacity = capacity;
            this.refillRatePerSec = refillRatePerSec;
            this.tokens = capacity; // Start with full capacity
            this.lastUpdate = System.currentTimeMillis() / 1000;
        }

        // Thread-safe token consumption logic
        synchronized boolean tryConsume() {
            long now = System.currentTimeMillis() / 1000;
            long elapsed = now - lastUpdate;

            // Refill tokens based on elapsed time
            tokens = Math.min(capacity, tokens + (elapsed * refillRatePerSec));
            lastUpdate = now;

            if (tokens >= 1.0) {
                tokens -= 1.0; // Deduct 1 token
                return true;  // Request allowed
            }
            return false;     // Token bucket empty, reject request
        }
    }

    // Thread-safe map holding buckets per Tenant ID
    private final ConcurrentHashMap<Object, Object> buckets = new ConcurrentHashMap<>();

    @Override
    public boolean allowRequest(String tenantId, int capacity, double refillRatePerSec) {
        TokenBucket bucket = (TokenBucket) buckets.computeIfAbsent(
            tenantId, 
            k -> new TokenBucket(capacity, refillRatePerSec)
        );
        return bucket.tryConsume();
    }
}