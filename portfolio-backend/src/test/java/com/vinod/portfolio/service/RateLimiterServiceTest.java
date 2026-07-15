package com.vinod.portfolio.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiterService();
    }

    @Test
    void firstFiveRequestsFromSameIpAreAllowed() {
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.tryConsume("1.2.3.4")).isTrue();
        }
    }

    @Test
    void sixthRequestFromSameIpIsBlocked() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryConsume("1.2.3.4");
        }
        assertThat(rateLimiter.tryConsume("1.2.3.4")).isFalse();
    }

    @Test
    void differentIpsHaveIndependentBuckets() {
        for (int i = 0; i < 5; i++) {
            rateLimiter.tryConsume("1.2.3.4");
        }
        assertThat(rateLimiter.tryConsume("5.6.7.8")).isTrue();
    }

    @Test
    void freshIpIsAlwaysAllowed() {
        assertThat(rateLimiter.tryConsume("9.9.9.9")).isTrue();
    }
}
