package cn.maarlakes.common.utils;


import java.time.Duration;

/**
 * @author linjpxc
 */
public interface RateLimiter {

    Long availablePermits();

    void acquire();

    boolean tryAcquire();

    boolean tryAcquire(Duration timeout);
}
