package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * HTTP 响应处理器，用于流式消费响应体。
 *
 * <p>与直接返回 {@link Response} 的模式不同，{@code ResponseHandler} 允许调用方
 * 在响应体流式传输过程中就进行处理，而不需要将完整响应体缓冲到内存。
 * 适用于大文件下载、流式解析等场景。
 *
 * <p>实现方接收响应元数据 {@link HttpResponse} 和流式响应体 {@link BodySink}，
 * 通过 {@link BodySink#consume} 注册 {@link BodyConsumer}
 * 来逐块消费数据。返回的 {@link CompletableFuture} 完成时表示处理结束。
 *
 * <p>标记为 {@link FunctionalInterface}，可以使用 Lambda 表达式。
 *
 * @param <T> 处理结果的类型
 * @author linjpxc
 */
@FunctionalInterface
public interface ResponseHandler<T> {

    /**
     * 处理 HTTP 响应。
     *
     * @param response 响应元数据（状态码、头部等），不包含响应体
     * @param body     流式响应体，通过 {@link BodySink#consume} 消费
     * @return 异步处理结果
     */
    CompletableFuture<T> handle(@Nonnull HttpResponse response, @Nonnull BodySink body);
}
