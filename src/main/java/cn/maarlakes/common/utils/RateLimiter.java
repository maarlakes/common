package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.time.Duration;

/**
 * @author linjpxc
 */
public interface RateLimiter {

    Long availablePermits();

    void acquire();

    boolean tryAcquire();

    boolean tryAcquire(@Nonnull Duration timeout);
}
