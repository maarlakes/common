package cn.maarlakes.common.function;

import jakarta.annotation.Nonnull;

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface Function2<T1, T2, R> extends BiFunction<T1, T2, R> {

    @Override
    default R apply(T1 t1, T2 t2) {
        return this.applyUnchecked(t1, t2);
    }

    default R applyUnchecked(T1 t1, T2 t2) {
        return this.applyUnchecked(t1, t2, Functions.THROWABLE_TO_RUNTIME_EXCEPTION);
    }

    default R applyUnchecked(T1 t1, T2 t2, @Nonnull Consumer<Throwable> handler) {
        try {
            return this.apply0(t1, t2);
        } catch (Throwable throwable) {
            handler.accept(throwable);
            throw new IllegalStateException(throwable);
        }
    }

    default R applyUnchecked(T1 t1, T2 t2, @Nonnull Function<Throwable, R> handler) {
        try {
            return this.apply0(t1, t2);
        } catch (Throwable throwable) {
            return handler.apply(throwable);
        }
    }

    R apply0(T1 t1, T2 t2) throws Throwable;

    @Nonnull
    default Function1<T2, R> acceptPartially(T1 t1) {
        return t2 -> this.apply0(t1, t2);
    }

    @Nonnull
    default Function0<R> acceptPartially(T1 t1, T2 t2) {
        return () -> this.apply0(t1, t2);
    }

    @Nonnull
    default BiFunction<T1, T2, R> toBiFunction() {
        return this::applyUnchecked;
    }

    @Nonnull
    static <T1, T2, R> Function2<T1, T2, R> from(@Nonnull BiFunction<T1, T2, R> biFunction) {
        return biFunction::apply;
    }
}
