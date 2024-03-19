package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface Consumer6<T1, T2, T3, T4, T5, T6> {

    default void acceptUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        this.acceptUnchecked(t1, t2, t3, t4, t5, t6, Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default void acceptUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6, @Nonnull Consumer<Throwable> handler) {
        try {
            this.accept(t1, t2, t3, t4, t5, t6);
        } catch (Throwable e) {
            handler.accept(e);
        }
    }

    void accept(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) throws Exception;

    @Nonnull
    default Consumer5<T2, T3, T4, T5, T6> acceptPartially(T1 t1) {
        return (t2, t3, t4, t5, t6) -> this.accept(t1, t2, t3, t4, t5, t6);
    }

    @Nonnull
    default Consumer4<T3, T4, T5, T6> acceptPartially(T1 t1, T2 t2) {
        return (t3, t4, t5, t6) -> this.accept(t1, t2, t3, t4, t5, t6);
    }

    @Nonnull
    default Consumer3<T4, T5, T6> acceptPartially(T1 t1, T2 t2, T3 t3) {
        return (t4, t5, t6) -> this.accept(t1, t2, t3, t4, t5, t6);
    }

    @Nonnull
    default Consumer2<T5, T6> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4) {
        return (t5, t6) -> this.accept(t1, t2, t3, t4, t5, t6);
    }

    @Nonnull
    default Consumer1<T6> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return (t6) -> this.accept(t1, t2, t3, t4, t5, t6);
    }

    @Nonnull
    default Consumer0 acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6) {
        return () -> this.accept(t1, t2, t3, t4, t5, t6);
    }
}
