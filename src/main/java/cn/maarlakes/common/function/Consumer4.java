package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface Consumer4<T1, T2, T3, T4> {

    default void acceptUnchecked(T1 t1, T2 t2, T3 t3, T4 t4) {
        this.acceptUnchecked(t1, t2, t3, t4, Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default void acceptUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, @Nonnull Consumer<Throwable> handler) {
        try {
            this.accept(t1, t2, t3, t4);
        } catch (Throwable e) {
            handler.accept(e);
        }
    }

    void accept(T1 t1, T2 t2, T3 t3, T4 t4) throws Throwable;

    @Nonnull
    default Consumer3<T2, T3, T4> acceptPartially(T1 t1) {
        return (t2, t3, t4) -> this.accept(t1, t2, t3, t4);
    }

    @Nonnull
    default Consumer2<T3, T4> acceptPartially(T1 t1, T2 t2) {
        return (t3, t4) -> this.accept(t1, t2, t3, t4);
    }

    @Nonnull
    default Consumer1<T4> acceptPartially(T1 t1, T2 t2, T3 t3) {
        return t4 -> this.accept(t1, t2, t3, t4);
    }

    @Nonnull
    default Consumer0 acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4) {
        return () -> this.accept(t1, t2, t3, t4);
    }
}
