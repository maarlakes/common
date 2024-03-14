package cn.maarlakes.common.function;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Consumer;

/**
 * @author linjpxc
 */
public class Functions {
    private Functions(){}

    public static final Consumer<Throwable> THROWABLE_TO_RUNTIME_EXCEPTION = throwable -> {
        if (throwable instanceof Error) {
            throw (Error) throwable;
        }
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException) throwable;
        }
        if (throwable instanceof IOException) {
            throw new UncheckedIOException((IOException) throwable);
        }
        if (throwable instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
        throw new UncheckedException(throwable);
    };
}
