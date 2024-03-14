package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface Consumer9<T1, T2, T3, T4, T5, T6, T7, T8, T9> {

    default void acceptUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
        this.acceptUnchecked(t1, t2, t3, t4, t5, t6, t7, t8, t9, Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default void acceptUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9, @Nonnull Consumer<Throwable> handler) {
        try {
            this.accept(t1, t2, t3, t4, t5, t6, t7, t8, t9);
        } catch (Throwable e) {
            handler.accept(e);
        }
    }

    void accept(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) throws Throwable;

    @Nonnull
    default Consumer8<T2, T3, T4, T5, T6, T7, T8, T9> acceptPartially(T1 t1) {
        return (t2, t3, t4, t5, t6, t7, t8, t9) -> this.accept(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Consumer7<T3, T4, T5, T6, T7, T8, T9> acceptPartially(T1 t1, T2 t2) {
        return (t3, t4, t5, t6, t7, t8, t9) -> this.accept(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Consumer6<T4, T5, T6, T7, T8, T9> acceptPartially(T1 t1, T2 t2, T3 t3) {
        return (t4, t5, t6, t7, t8, t9) -> this.accept(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Consumer5<T5, T6, T7, T8, T9> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4) {
        return (t5, t6, t7, t8, t9) -> this.accept(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Consumer4<T6, T7, T8, T9> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return (t6, t7, t8, t9) -> this.accept(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Consumer3<T7, T8, T9> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        return (t7, t8, t9) -> this.accept(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Consumer2<T8, T9> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7) {
        return (t8, t9) -> this.accept(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Consumer1<T9> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8) {
        return (t9) -> this.accept(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }

    @Nonnull
    default Consumer0 acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, T7 t7, T8 t8, T9 t9) {
        return () -> this.accept(t1, t2, t3, t4, t5, t6, t7, t8, t9);
    }
}
