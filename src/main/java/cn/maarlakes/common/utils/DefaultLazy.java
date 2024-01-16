package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * @author linjpxc
 */
final class DefaultLazy<T> implements Lazy<T> {

    private final Supplier<T> factory;
    private volatile T value = null;
    private final Object lock = new Object();

    DefaultLazy(@Nonnull Supplier<T> factory) {
        this.factory = factory;
    }

    @Override
    public boolean isCreated() {
        return this.value != null;
    }

    @Override
    public T get() {
        if (this.value == null) {
            synchronized (this.lock) {
                if (this.value == null) {
                    this.value = this.factory.get();
                }
            }
        }
        return this.value;
    }
}
