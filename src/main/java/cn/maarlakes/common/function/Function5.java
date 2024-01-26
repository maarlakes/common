package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface Function5<T1, T2, T3, T4, T5, R> {

    default R applyUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return this.applyUnchecked(t1, t2, t3, t4, t5, Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default R applyUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, @Nonnull Consumer<Throwable> handler) {
        try {
            return this.apply(t1, t2, t3, t4, t5);
        } catch (Throwable throwable) {
            handler.accept(throwable);
            throw new IllegalStateException(throwable);
        }
    }

    default R applyUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, @Nonnull Function<Throwable, R> handler) {
        try {
            return this.apply(t1, t2, t3, t4, t5);
        } catch (Throwable throwable) {
            return handler.apply(throwable);
        }
    }

    R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) throws Throwable;

    @Nonnull
    default Function4<T2, T3, T4, T5, R> acceptPartially(T1 t1) {
        return (t2, t3, t4, t5) -> this.apply(t1, t2, t3, t4, t5);
    }

    @Nonnull
    default Function3<T3, T4, T5, R> acceptPartially(T1 t1, T2 t2) {
        return (t3, t4, t5) -> this.apply(t1, t2, t3, t4, t5);
    }

    @Nonnull
    default Function2<T4, T5, R> acceptPartially(T1 t1, T2 t2, T3 t3) {
        return (t4, t5) -> this.apply(t1, t2, t3, t4, t5);
    }

    @Nonnull
    default Function1<T5, R> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4) {
        return (t5) -> this.apply(t1, t2, t3, t4, t5);
    }

    @Nonnull
    default Function0<R> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return () -> this.apply(t1, t2, t3, t4, t5);
    }
}
