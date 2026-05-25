package cn.maarlakes.common.utils;


import java.util.function.Supplier;

/**
 * @author linjpxc
 */
class DefaultLazy<T> implements Lazy<T> {

    private final Supplier<T> factory;
    private T value = null;
    private volatile boolean isCreated = false;
    private final Object lock = new Object();

    DefaultLazy(Supplier<T> factory) {
        this.factory = factory;
    }

    @Override
    public boolean isCreated() {
        return this.isCreated;
    }

    @Override
    public T apply() throws Exception {
        if (!this.isCreated) {
            synchronized (this.lock) {
                if (!this.isCreated) {
                    this.value = this.factory.get();
                    this.isCreated = true;
                }
            }
        }
        return this.value;
    }
}
