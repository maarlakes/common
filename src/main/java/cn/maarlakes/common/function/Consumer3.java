package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface Consumer3<T1, T2, T3> {

    default void acceptUnchecked(T1 t1, T2 t2, T3 t3) {
        this.acceptUnchecked(t1, t2, t3, Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default void acceptUnchecked(T1 t1, T2 t2, T3 t3, @Nonnull Consumer<Throwable> handler) {
        try {
            this.accept(t1, t2, t3);
        } catch (Throwable e) {
            handler.accept(e);
        }
    }

    void accept(T1 t1, T2 t2, T3 t3) throws Exception;

    @Nonnull
    default Consumer2<T2, T3> acceptPartially(T1 t1) {
        return (t2, t3) -> this.accept(t1, t2, t3);
    }

    @Nonnull
    default Consumer1<T3> acceptPartially(T1 t1, T2 t2) {
        return t3 -> this.accept(t1, t2, t3);
    }

    @Nonnull
    default Consumer0 acceptPartially(T1 t1, T2 t2, T3 t3) {
        return () -> this.accept(t1, t2, t3);
    }
}
