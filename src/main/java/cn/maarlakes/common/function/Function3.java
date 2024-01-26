package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface Function3<T1, T2, T3, R> {

    default R applyUnchecked(T1 t1, T2 t2, T3 t3) {
        return this.applyUnchecked(t1, t2, t3, Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default R applyUnchecked(T1 t1, T2 t2, T3 t3, @Nonnull Consumer<Throwable> handler) {
        try {
            return this.apply(t1, t2, t3);
        } catch (Throwable throwable) {
            handler.accept(throwable);
            throw new IllegalStateException(throwable);
        }
    }

    default R applyUnchecked(T1 t1, T2 t2, T3 t3, @Nonnull Function<Throwable, R> handler) {
        try {
            return this.apply(t1, t2, t3);
        } catch (Throwable throwable) {
            return handler.apply(throwable);
        }
    }

    R apply(T1 t1, T2 t2, T3 t3) throws Throwable;

    @Nonnull
    default Function2<T2, T3, R> acceptPartially(T1 t1) {
        return (t2, t3) -> this.apply(t1, t2, t3);
    }

    @Nonnull
    default Function1<T3, R> acceptPartially(T1 t1, T2 t2) {
        return (t3) -> this.apply(t1, t2, t3);
    }

    @Nonnull
    default Function0<R> acceptPartially(T1 t1, T2 t2, T3 t3) {
        return () -> this.apply(t1, t2, t3);
    }
}
