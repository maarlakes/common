package cn.maarlakes.common.load;

import cn.maarlakes.common.utils.CollectionUtils;
import cn.maarlakes.common.utils.Weights;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.Random;

/**
 * @author linjpxc
 */
public class WeightRandomLoadBalancing<T> implements LoadBalancing<T> {

    private final Weights<T>[] array;
    private final Random random;

    private final int totalWeights;

    public WeightRandomLoadBalancing(@Nonnull Weights<T>[] array) {
        this(array, new Random());
    }

    public WeightRandomLoadBalancing(@Nonnull Weights<T>[] array, @Nonnull Random random) {
        this.array = array;
        this.random = random;
        if (this.array.length < 1) {
            throw new IllegalArgumentException("array is null or empty");
        }
        this.totalWeights = Arrays.stream(this.array).mapToInt(Weights::weight).sum();
    }

    public WeightRandomLoadBalancing(@Nonnull Iterable<? extends Weights<T>> iterable) {
        this(iterable, new Random());
    }

    @SuppressWarnings("unchecked")
    public WeightRandomLoadBalancing(@Nonnull Iterable<? extends Weights<T>> iterable, @Nonnull Random random) {
        this.array = CollectionUtils.stream(iterable, false).toArray(Weights[]::new);
        this.random = random;
        if (this.array.length < 1) {
            throw new IllegalArgumentException("iterable is null or empty");
        }

        this.totalWeights = Arrays.stream(this.array).mapToInt(Weights::weight).sum();
    }

    @Nonnull
    @Override
    public T select() {
        final int weight = this.random.nextInt(this.totalWeights);
        int currentWeight = 0;
        for (Weights<T> item : this.array) {
            currentWeight += item.weight();
            if (weight < currentWeight) {
                return item.target();
            }
        }
        return this.array[weight % this.array.length].target();
    }
}
