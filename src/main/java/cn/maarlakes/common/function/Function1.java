package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface Function1<T1, R> extends Function<T1, R> {

    @Override
    default R apply(T1 t1) {
        return this.applyUnchecked(t1);
    }

    default R applyUnchecked(T1 t1) {
        return this.applyUnchecked(t1, Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default R applyUnchecked(T1 t1, @Nonnull Consumer<Throwable> handler) {
        try {
            return this.apply0(t1);
        } catch (Throwable throwable) {
            handler.accept(throwable);
            throw new IllegalStateException(throwable);
        }
    }

    default R applyUnchecked(T1 t1, @Nonnull Function<Throwable, R> handler) {
        try {
            return this.apply0(t1);
        } catch (Throwable throwable) {
            return handler.apply(throwable);
        }
    }

    R apply0(T1 t1) throws Throwable;

    @Nonnull
    default Function0<R> acceptPartially(T1 t1) {
        return () -> this.apply0(t1);
    }

    @Nonnull
    default Function<T1, R> toFunction() {
        return this::applyUnchecked;
    }

    @Nonnull
    static <T1, R> Function1<T1, R> from(@Nonnull Function<T1, R> function) {
        return function::apply;
    }

    static <T> Function1<T, T> identity() {
        return t -> t;
    }
}
