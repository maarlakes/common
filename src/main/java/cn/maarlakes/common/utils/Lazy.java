package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * @author linjpxc
 */
public interface Lazy<T> extends Supplier<T> {

    boolean isCreated();

    @Nonnull
    static <T> Lazy<T> of(@Nonnull Supplier<T> factory) {
        if (factory instanceof Lazy) {
            return (Lazy<T>) factory;
        }
        return new DefaultLazy<>(factory);
    }
}
