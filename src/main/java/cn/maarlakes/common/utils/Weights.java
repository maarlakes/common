package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * @author linjpxc
 */
public final class Weights<T> implements Serializable {
    private static final long serialVersionUID = 949174082420985235L;

    private final T target;
    private final int weight;

    public Weights(@Nonnull T target, int weight) {
        this.target = target;
        this.weight = weight;
    }

    @Nonnull
    public T target() {
        return target;
    }

    public int weight() {
        return weight;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Weights)) {
            return false;
        }

        final Weights<?> weights = (Weights<?>) o;
        return weight == weights.weight && target.equals(weights.target);
    }

    @Override
    public int hashCode() {
        int result = target.hashCode();
        result = 31 * result + weight;
        return result;
    }

    @Override
    public String toString() {
        return this.target + ": " + this.weight;
    }
}
