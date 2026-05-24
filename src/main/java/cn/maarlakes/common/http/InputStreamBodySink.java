package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * @author linjpxc
 */
public class InputStreamBodySink implements BodySink {
    private final InputStream inputStream;

    public InputStreamBodySink(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    @Override
    public <T> CompletableFuture<T> consume(@Nonnull BodyConsumer<T> consumer) {
        final CompletableFuture<T> future = new CompletableFuture<>();
        if (this.inputStream == null) {
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
        try (InputStream in = this.inputStream) {
            final byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                consumer.onChunk(buffer, 0, n);
            }
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
}
