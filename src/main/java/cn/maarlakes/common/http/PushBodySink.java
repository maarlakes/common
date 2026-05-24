package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author linjpxc
 */
public class PushBodySink implements BodySink {
    private final List<byte[]> buffer = new ArrayList<>();
    private BodyConsumer<?> consumer;
    private boolean completed;
    private Throwable error;
    private final CompletableFuture<Void> signal = new CompletableFuture<>();

    @Override
    public synchronized <T> CompletableFuture<T> consume(@Nonnull BodyConsumer<T> consumer) {
        this.consumer = consumer;
        for (byte[] chunk : buffer) {
            consumer.onChunk(chunk, 0, chunk.length);
        }
        buffer.clear();

        if (error != null) {
            try {
                consumer.onError(error);
            } catch (Exception onErrorEx) {
                error.addSuppressed(onErrorEx);
            }
            final CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(error);
            return future;
        }
        if (completed) {
            final CompletableFuture<T> future = new CompletableFuture<>();
            try {
                future.complete(consumer.onComplete());
            } catch (Exception e) {
                try {
                    consumer.onError(e);
                } catch (Exception onErrorEx) {
                    e.addSuppressed(onErrorEx);
                }
                future.completeExceptionally(e);
            }
            return future;
        }

        final CompletableFuture<T> future = new CompletableFuture<>();
        signal.whenComplete((v, ex) -> {
            if (ex != null) {
                try {
                    consumer.onError(ex);
                } catch (Exception onErrorEx) {
                    ex.addSuppressed(onErrorEx);
                }
                future.completeExceptionally(ex);
            } else {
                try {
                    future.complete(consumer.onComplete());
                } catch (Exception e) {
                    try {
                        consumer.onError(e);
                    } catch (Exception onErrorEx) {
                        e.addSuppressed(onErrorEx);
                    }
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
    }

    public synchronized void pushChunk(byte[] data, int offset, int length) {
        final byte[] copy = Arrays.copyOfRange(data, offset, offset + length);
        if (consumer != null) {
            consumer.onChunk(copy, 0, copy.length);
        } else {
            buffer.add(copy);
        }
    }

    public void complete() {
        synchronized (this) {
            completed = true;
        }
        signal.complete(null);
    }

    public void fail(Throwable t) {
        synchronized (this) {
            error = t;
        }
        signal.completeExceptionally(t);
    }
}
