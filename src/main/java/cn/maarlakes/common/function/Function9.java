package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author linjpxc
 */
public interface Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, R> {

    default R applyUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
        return this.applyUnchecked(t1, t2, t3, t4, t5, t6, t7, t8, t9, Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default R applyUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, @Nonnull Consumer<Throwable> handler) {
        try {
            return this.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
        } catch (Throwable throwable) {
            handler.accept(throwable);
            throw new IllegalStateException(throwable);
        }
    }

    default R applyUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, @Nonnull Function<Throwable, R> handler) {
        try {
            return this.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
        } catch (Throwable throwable) {
            return handler.apply(throwable);
        }
    }

    R apply(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) throws Exception;

    @Nonnull
    default Function8<T2, T3, T4, T5, T6, T7, T8, T9, R> acceptPartially(T1 t1) {
        return (t2, t3, t4, t5, t6, t7, t8, t9) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Function7<T3, T4, T5, T6, T7, T8, T9, R> acceptPartially(T1 t1, T2 t2) {
        return (t3, t4, t5, t6, t7, t8, t9) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Function6<T4, T5, T6, T7, T8, T9, R> acceptPartially(T1 t1, T2 t2, T3 t3) {
        return (t4, t5, t6, t7, t8, t9) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Function5<T5, T6, T7, T8, T9, R> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4) {
        return (t5, t6, t7, t8, t9) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Function4<T6, T7, T8, T9, R> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return (t6, t7, t8, t9) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Function3<T7, T8, T9, R> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        return (t7, t8, t9) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Function2<T8, T9, R> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
        return (t8, t9) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Function1<T9, R> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
        return (t9) -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Function0<R> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
        return () -> this.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }
}
