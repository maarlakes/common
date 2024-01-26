package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author linjpxc
 */
public interface Consumer5<T1, T2, T3, T4, T5> {

    default void acceptUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        this.acceptUnchecked(t1, t2, t3, t4, t5, Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default void acceptUnchecked(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, @Nonnull Consumer<Throwable> handler) {
        try {
            this.accept(t1, t2, t3, t4, t5);
        } catch (Throwable e) {
            handler.accept(e);
        }
    }

    void accept(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) throws Throwable;

    @Nonnull
    default Consumer4<T2, T3, T4, T5> acceptPartially(T1 t1) {
        return (t2, t3, t4, t5) -> this.accept(t1, t2, t3, t4, t5);
    }

    @Nonnull
    default Consumer3<T3, T4, T5> acceptPartially(T1 t1, T2 t2) {
        return (t3, t4, t5) -> this.accept(t1, t2, t3, t4, t5);
    }

    @Nonnull
    default Consumer2<T4, T5> acceptPartially(T1 t1, T2 t2, T3 t3) {
        return (t4, t5) -> this.accept(t1, t2, t3, t4, t5);
    }

    @Nonnull
    default Consumer1<T5> acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4) {
        return t5 -> this.accept(t1, t2, t3, t4, t5);
    }

    @Nonnull
    default Consumer0 acceptPartially(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return () -> this.accept(t1, t2, t3, t4, t5);
    }
}
