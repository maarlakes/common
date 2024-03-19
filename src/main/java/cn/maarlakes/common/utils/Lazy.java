package cn.maarlakes.common.utils;

import cn.maarlakes.common.function.Function0;
import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * @author linjpxc
 */
public interface Lazy<T> extends Function0<T> {

    boolean isCreated();

    @Nonnull
    static <T> Lazy<T> of(@Nonnull Function0<T> factory) {
        if (factory instanceof Lazy) {
            return (Lazy<T>) factory;
        }
        return new DefaultLazy<>(factory);
    }

    @Nonnull
    static <T> Lazy<T> of(@Nonnull Supplier<T> factory){
        if (factory instanceof Lazy) {
            return (Lazy<T>) factory;
        }
        return new DefaultLazy<>(factory);
    }
}
