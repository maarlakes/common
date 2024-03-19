package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface Consumer2<T1, T2> extends BiConsumer<T1, T2> {

    @Override
    default void accept(T1 t1, T2 t2) {
        this.acceptUnchecked(t1, t2);
    }

    default void acceptUnchecked(T1 t1, T2 t2) {
        this.acceptUnchecked(t1, t2, Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default void acceptUnchecked(T1 t1, T2 t2, @Nonnull Consumer<Throwable> handler) {
        try {
            this.accept0(t1, t2);
        } catch (Throwable e) {
            handler.accept(e);
        }
    }

    void accept0(T1 t1, T2 t2) throws Exception;

    @Nonnull
    default Consumer1<T2> acceptPartially(T1 t1) {
        return t2 -> this.accept0(t1, t2);
    }

    @Nonnull
    default Consumer0 acceptPartially(T1 t1, T2 t2) {
        return () -> this.accept0(t1, t2);
    }

    @Nonnull
    default BiConsumer<T1, T2> toBiConsumer() {
        return this::acceptUnchecked;
    }

    @Nonnull
    static <T1, T2> Consumer2<T1, T2> from(@Nonnull BiConsumer<T1, T2> consumer) {
        return consumer::accept;
    }
}
