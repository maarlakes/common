package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface Function6<T1, T2, T3, T4, T5, T6, R> {

    default R applyUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        return this.applyUnchecked(t1, t2, t3, t4, t5, t6, Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default R applyUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, @Nonnull Consumer<Throwable> handler) {
        try {
            return this.apply(t1, t2, t3, t4, t5, t6);
        } catch (Throwable throwable) {
            handler.accept(throwable);
            throw new IllegalStateException(throwable);
        }
    }

    default R applyUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, @Nonnull Function<Throwable, R> handler) {
        try {
            return this.apply(t1, t2, t3, t4, t5, t6);
        } catch (Throwable throwable) {
            return handler.apply(throwable);
        }
    }

    R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) throws Throwable;

    @Nonnull
    default Function5<T2, T3, T4, T5, T6, R> acceptPartially(T1 t1) {
        return (t2, t3, t4, t5, t6) -> this.apply(t1, t2, t3, t4, t5, t6);
    }

    @Nonnull
    default Function4<T3, T4, T5, T6, R> acceptPartially(T1 t1, T2 t2) {
        return (t3, t4, t5, t6) -> this.apply(t1, t2, t3, t4, t5, t6);
    }

    @Nonnull
    default Function3<T4, T5, T6, R> acceptPartially(T1 t1, T2 t2, T3 t3) {
        return (t4, t5, t6) -> this.apply(t1, t2, t3, t4, t5, t6);
    }

    @Nonnull
    default Function2<T5, T6, R> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4) {
        return (t5, t6) -> this.apply(t1, t2, t3, t4, t5, t6);
    }

    @Nonnull
    default Function1<T6, R> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return (t6) -> this.apply(t1, t2, t3, t4, t5, t6);
    }

    @Nonnull
    default Function0<R> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        return () -> this.apply(t1, t2, t3, t4, t5, t6);
    }
}
