package cn.maarlakes.common.load;

import cn.maarlakes.common.utils.ArrayUtils;
import jakarta.annotation.Nonnull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author linjpxc
 */
public class RoundRobinLoadBalancing<T> implements LoadBalancing<T> {

    private final T[] array;
    private final AtomicInteger index = new AtomicInteger(0);

    public RoundRobinLoadBalancing(@Nonnull T[] array) {
        this.array = array;
        if (this.array.length < 1) {
            throw new IllegalArgumentException("array is null or empty");
        }
    }

    public RoundRobinLoadBalancing(@Nonnull Iterable<? extends T> iterable) {
        this.array = ArrayUtils.toArray(iterable);
        if (this.array.length < 1) {
            throw new IllegalArgumentException("iterable is null or empty");
        }
    }

    @Nonnull
    @Override
    public T select() {
        final int i = this.index.getAndIncrement();
        return this.array[Math.abs(i) % this.array.length];
    }
}
