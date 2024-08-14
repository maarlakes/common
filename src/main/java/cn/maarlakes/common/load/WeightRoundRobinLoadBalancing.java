package cn.maarlakes.common.load;

import cn.maarlakes.common.utils.ArrayUtils;
import cn.maarlakes.common.utils.Weights;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author linjpxc
 */
public class WeightRoundRobinLoadBalancing<T> implements LoadBalancing<T> {

    private final Weights<T>[] array;
    private final AtomicInteger index = new AtomicInteger(0);
    private final int totalWeight;

    @SuppressWarnings("unchecked")
    public WeightRoundRobinLoadBalancing(@Nonnull Weights<? extends T>[] array) {
        this.array = (Weights<T>[]) array;
        if (this.array.length < 1) {
            throw new IllegalArgumentException("array is null or empty");
        }
        this.totalWeight = Arrays.stream(this.array).mapToInt(Weights::weight).sum();
    }

    public WeightRoundRobinLoadBalancing(@Nonnull Iterable<? extends Weights<T>> iterable) {
        this(ArrayUtils.toArray(iterable));
    }

    @Nonnull
    @Override
    public T select() {
        int currentIndex = Math.abs(this.index.getAndIncrement()) % this.totalWeight;
        for (Weights<T> weights : this.array) {
            if (currentIndex < weights.weight()) {
                return weights.target();
            }
            currentIndex -= weights.weight();
        }
        return this.array[currentIndex % this.array.length].target();
    }
}
