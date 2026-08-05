package com.gateway.limiter;

public interface RateLimiter {
	
    boolean allowRequest(String tenantId, int capacity, double refillRatePerSec);
}
