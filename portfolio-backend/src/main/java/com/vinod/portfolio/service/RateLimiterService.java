package com.vinod.portfolio.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {

    // 5 submissions per IP per hour
    private static final int CAPACITY = 5;
    private static final Duration REFILL_PERIOD = Duration.ofHours(1);

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public boolean tryConsume(String ip) {
        return buckets.computeIfAbsent(ip, this::newBucket).tryConsume(1);
    }

    private Bucket newBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(CAPACITY, Refill.intervally(CAPACITY, REFILL_PERIOD));
        return Bucket.builder().addLimit(limit).build();
    }
}
