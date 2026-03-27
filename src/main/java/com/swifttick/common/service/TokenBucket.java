package com.swifttick.common.service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class TokenBucket {
    private final long capacity;
    private final long refillRate; // tokens per second
    private final AtomicLong tokens;
    private volatile Instant lastRefill;

    public TokenBucket(long capacity, long refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = new AtomicLong(capacity);
        this.lastRefill = Instant.now();
    }

    public synchronized boolean tryConsume() {
        refill();
        if (tokens.get() > 0) {
            tokens.decrementAndGet();
            return true;
        }
        return false;
    }

    private void refill() {
        Instant now = Instant.now();
        long secondsElapsed = now.getEpochSecond() - lastRefill.getEpochSecond();
        if (secondsElapsed > 0) {
            long tokensToAdd = secondsElapsed * refillRate;
            if (tokensToAdd > 0) {
                long currentTokens = tokens.get();
                long newTokens = Math.min(capacity, currentTokens + tokensToAdd);
                tokens.set(newTokens);
                lastRefill = now;
            }
        }
    }
}
