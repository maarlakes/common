package cn.maarlakes.common.load;

import cn.maarlakes.common.utils.ArrayUtils;
import jakarta.annotation.Nonnull;

import java.util.Random;

/**
 * @author linjpxc
 */
public class RandomLoadBalancing<T> implements LoadBalancing<T> {

    private final Random random;
    private final T[] array;

    public RandomLoadBalancing(@Nonnull Iterable<? extends T> iterable) {
        this(iterable, new Random());
    }

    public RandomLoadBalancing(@Nonnull Iterable<? extends T> iterable, @Nonnull Random random) {
        this.array = ArrayUtils.toArray(iterable);
        this.random = random;
        if (this.array.length < 1){
            throw new IllegalArgumentException("iterable is null or empty");
        }
    }

    public RandomLoadBalancing(@Nonnull T[] array) {
        this(array, new Random());
    }

    public RandomLoadBalancing(@Nonnull T[] array, @Nonnull Random random) {
        if (array.length < 1) {
            throw new IllegalArgumentException("array is null or empty");
        }
        this.array = array;
        this.random = random;
    }

    @Nonnull
    @Override
    public T select() {
        return array[random.nextInt(array.length)];
    }
}
