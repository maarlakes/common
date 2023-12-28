package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * @author linjpxc
 */
public final class Lazy {
    private Lazy() {
    }

    @Nonnull
    public static <T> Supplier<T> of(@Nonnull Supplier<T> action) {
        return new Supplier<T>() {

            private volatile T value = null;
            private final Object sync = new Object();

            @Override
            public T get() {
                if (this.value == null) {
                    synchronized (this.sync) {
                        if (this.value == null) {
                            this.value = action.get();
                        }
                    }
                }
                return this.value;
            }
        };
    }
}
