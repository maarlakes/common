package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface BodyConsumer<T> {

    void onChunk(@Nonnull byte[] data, int offset, int length);

    T onComplete();

    default void onError(@Nonnull Throwable error) {
        if (error instanceof RuntimeException) {
            throw (RuntimeException) error;
        }
        if (error instanceof Error) {
            throw (Error) error;
        }
        throw new RuntimeException(error);
    }
}
