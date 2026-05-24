package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 推送模式的 {@link BodySink} 实现，支持从外部逐步推送数据块。
 *
 * <p>适用于异步 HTTP 客户端（如 Apache AsyncHttpClient、OkHttp）的场景，
 * 响应体数据从网络层逐步到达，通过 {@link #pushChunk} 推入缓冲区。
 * 当消费者尚未注册时，数据块会被暂存在内部缓冲区中；
 * 消费者注册后，暂存的数据会被重放。通过 {@link CompletableFuture} 信号量
 * 实现生产者-消费者的完成/失败同步。</p>
 *
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

    /**
     * 从外部推送一个数据块。若消费者已注册则直接传递，否则暂存在缓冲区中。
     */
    public synchronized void pushChunk(byte[] data, int offset, int length) {
        final byte[] copy = Arrays.copyOfRange(data, offset, offset + length);
        if (consumer != null) {
            consumer.onChunk(copy, 0, copy.length);
        } else {
            buffer.add(copy);
        }
    }

    /**
     * 标记响应体数据传输完成，通知等待中的消费者。
     */
    public void complete() {
        synchronized (this) {
            completed = true;
        }
        signal.complete(null);
    }

    /**
     * 标记响应体传输失败，将异常传播给等待中的消费者。
     */
    public void fail(Throwable t) {
        synchronized (this) {
            error = t;
        }
        signal.completeExceptionally(t);
    }
}
