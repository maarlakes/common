package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface Consumer0 extends Runnable {

    @Override
    default void run() {
        this.acceptUnchecked();
    }

    default void acceptUnchecked() {
        this.acceptUnchecked(Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default void acceptUnchecked(@Nonnull Consumer<Throwable> handler) {
        try {
            this.accept();
        } catch (Throwable e) {
            handler.accept(e);
        }
    }

    void accept() throws Throwable;

    @Nonnull
    default Runnable toRunnable() {
        return this::acceptUnchecked;
    }

    @Nonnull
    static Consumer0 from(@Nonnull Runnable runnable) {
        return runnable::run;
    }
}
