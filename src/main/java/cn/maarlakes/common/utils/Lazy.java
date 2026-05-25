package cn.maarlakes.common.utils;

import cn.maarlakes.common.function.Function0;

import java.util.function.Supplier;

/**
 * @author linjpxc
 */
public interface Lazy<T> extends Function0<T> {

    boolean isCreated();

    static <T> Lazy<T> of(Function0<T> factory) {
        if (factory instanceof Lazy) {
            return (Lazy<T>) factory;
        }
        return new DefaultLazy<>(factory);
    }

    static <T> Lazy<T> of(Supplier<T> factory) {
        if (factory instanceof Lazy) {
            return (Lazy<T>) factory;
        }
        return new DefaultLazy<>(factory);
    }
}
