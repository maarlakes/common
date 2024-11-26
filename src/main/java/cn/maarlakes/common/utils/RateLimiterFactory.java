package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface RateLimiterFactory {

    @Nonnull
    RateLimiter createLimiter();
}
