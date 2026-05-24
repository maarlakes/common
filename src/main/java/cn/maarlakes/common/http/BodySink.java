package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * 响应体消费入口，通过注册 {@link BodyConsumer} 来流式消费响应体。
 *
 * <p>与 {@link Response} 的缓冲模式不同，{@code BodySink} 只能被消费一次。
 * 调用 {@link #consume} 后，底层流将逐块传递给 {@code BodyConsumer} 的回调方法，
 * 消费完成后返回处理结果。
 *
 * <p>适用于大文件下载、流式解析等不宜将完整响应体缓冲到内存的场景。
 * 在 {@link ResponseHandler} 的 {@code handle} 方法中通过第二个参数传入。
 *
 * @author linjpxc
 */
public interface BodySink {

    /**
     * 注册消费者并开始消费响应体流。
     *
     * <p>此方法只能调用一次。底层流将逐块传递给消费者的回调方法，
     * 全部消费完毕后调用 {@link BodyConsumer#onComplete()} 返回结果。
     *
     * @param consumer 响应体消费者
     * @param <T>      消费者的返回类型
     * @return 异步处理结果，完成后包含消费者返回的值
     */
    <T> CompletableFuture<T> consume(@Nonnull BodyConsumer<T> consumer);
}
