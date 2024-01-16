package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.util.function.Supplier;

/**
 * @author linjpxc
 */
public interface Lazy<T> extends Supplier<T> {

    boolean isCreated();

    @Nonnull
//    static <T> Lazy<T> of(@Nonnull Supplier<T> factory) {
//        return new DefaultLazy<>(factory);
//    }

    static <T> Supplier<T> of(@Nonnull Supplier<T> factory) {
        return new Supplier<T>() {

            private volatile T value = null;
            private final Object sync = new Object();

            @Override
            public T get() {
                if (this.value == null) {
                    synchronized (this.sync) {
                        if (this.value == null) {
                            this.value = factory.get();
                        }
                    }
                }
                return this.value;
            }
        };
    }
}
