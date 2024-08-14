package cn.maarlakes.common.load;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface LoadBalancing<T> {

    @Nonnull
    T select();
}
