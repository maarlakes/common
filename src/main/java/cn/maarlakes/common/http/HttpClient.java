package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * HTTP 客户端顶层接口，定义异步请求执行的统一契约。
 *
 * <p>所有实现均以 {@link CompletableFuture} 返回结果，不提供同步阻塞 API。
 * 框架内置多种实现（Apache HC 4/5、OkHttp、AsyncHttpClient、JDK HttpURLConnection），
 * 通过 {@link HttpClientFactory} SPI 自动发现，亦可通过 {@link #builder()} 手动构建。
 *
 * <p>实现 {@link AutoCloseable}，持有连接池等资源的实现应在 {@link #close()} 中释放。
 * {@code close()} 只抛 {@link RuntimeException}，不抛受检异常，方便在 try-with-resources 中使用。
 *
 * @author linjpxc
 */
public interface HttpClient extends AutoCloseable {

    /**
     * 使用默认配置发送请求。
     *
     * @param request HTTP 请求，不允许为 null
     * @return 异步响应，完成后包含完整的 {@link Response}
     */
    @Nonnull
    default CompletableFuture<Response> execute(@Nonnull Request request) {
        return execute(request, null);
    }

    /**
     * 使用指定配置发送请求。{@code config} 为 null 时使用客户端默认配置。
     *
     * @param request HTTP 请求，不允许为 null
     * @param config  请求级配置（超时、代理等），可为 null
     * @return 异步响应，完成后包含完整的 {@link Response}
     */
    @Nonnull
    CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config);

    /**
     * 使用指定配置发送请求，并通过 {@link ResponseHandler} 流式处理响应体。
     *
     * <p>与 {@link #execute(Request, RequestConfig)} 不同，此方法不会将响应体完整缓冲到内存，
     * 而是将底层流交给 {@code handler} 自行消费，适合处理大文件或流式场景。
     *
     * @param request HTTP 请求，不允许为 null
     * @param config  请求级配置，可为 null
     * @param handler 响应处理器，不允许为 null
     * @param <T>     handler 的返回类型
     * @return 异步结果，完成后包含 handler 的处理结果
     */
    @Nonnull
    <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler);

    /**
     * 释放底层资源（连接池、线程池、事件循环等）。
     *
     * <p>调用后不应再执行新请求。只抛 {@link RuntimeException}，不抛受检异常。
     */
    @Override
    void close() throws RuntimeException;

    /**
     * 创建 {@link HttpClientBuilder} 以流式构建客户端实例。
     *
     * <p>未指定 {@link HttpClientFactory} 时，默认创建基于 JDK HttpURLConnection 的实现。
     *
     * @return 新的 Builder 实例
     */
    @Nonnull
    static HttpClientBuilder builder() {
        return new DefaultHttpClientBuilder();
    }
}
