package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface Consumer1<T> extends Consumer<T> {

    @Override
    default void accept(T t) {
        this.acceptUnchecked(t);
    }

    default void acceptUnchecked(T t) {
        this.acceptUnchecked(t, Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default void acceptUnchecked(T t, @Nonnull Consumer<Throwable> handler) {
        try {
            this.accept0(t);
        } catch (Throwable e) {
            handler.accept(e);
        }
    }

    void accept0(T t) throws Throwable;

    default Consumer0 acceptPartially(T t) {
        return () -> this.accept0(t);
    }

    @Nonnull
    default Consumer<T> toConsumer() {
        return this::acceptUnchecked;
    }

    @Nonnull
    static <T> Consumer1<T> from(@Nonnull Consumer<T> consumer) {
        return consumer::accept;
    }
}
