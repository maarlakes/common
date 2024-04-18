package cn.maarlakes.common.function;


import jakarta.annotation.Nonnull;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface Function0<R> extends Supplier<R>, Callable<R> {

    default R applyUnchecked() {
        return this.applyUnchecked(Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default R applyUnchecked(@Nonnull Consumer<Throwable> handler) {
        try {
            return this.apply();
        } catch (Throwable throwable) {
            handler.accept(throwable);
            throw new IllegalStateException(throwable);
        }
    }

    default R applyUnchecked(@Nonnull Function<Throwable, R> handler) {
        try {
            return this.apply();
        } catch (Throwable throwable) {
            return handler.apply(throwable);
        }
    }

    R apply() throws Exception;

    @Override
    default R call() throws Exception {
        return this.apply();
    }

    @Override
    default R get() {
        return this.applyUnchecked();
    }

    @Nonnull
    default Supplier<R> toSupplier() {
        return this::applyUnchecked;
    }

    @Nonnull
    default Callable<R> toCallable() {
        return this::applyUnchecked;
    }

    @Nonnull
    static <R> Function0<R> from(@Nonnull Supplier<R> supplier) {
        return supplier::get;
    }

    @Nonnull
    static <R> Function0<R> from(@Nonnull Callable<R> callable) {
        return callable::call;
    }
}
