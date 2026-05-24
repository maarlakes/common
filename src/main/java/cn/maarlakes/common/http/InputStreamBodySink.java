package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

/**
 * 基于 {@link InputStream} 的 {@link BodySink} 实现，从输入流中逐块读取数据并传递给消费者。
 *
 * <p>适用于响应体已完整存在于输入流中的场景（如 JDK HttpURLConnection）。
 * 使用 8KB 的固定缓冲区逐块读取，并在完成后自动关闭输入流。
 * 当输入流为 null 时直接调用消费者的 {@code onComplete()} 方法。</p>
 *
 * @author linjpxc
 */
public class InputStreamBodySink implements BodySink {
    private final InputStream inputStream;

    /**
     * @param inputStream 响应体输入流，可以为 {@code null}（表示空响应体）
     */
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
