package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

/**
 * HTTP 完整响应，包含状态码、头部和已缓冲的响应体。
 *
 * <p>与 {@link HttpResponse} 的区别：{@code Response} 包含完整可重复读取的 {@link ResponseBody}，
 * 而 {@link HttpResponse} 是流式响应，只能通过 {@link ResponseHandler} 一次性消费。
 *
 * <p>提供了 HTTP 状态码分类的便捷方法：
 * <ul>
 *   <li>{@link #isSuccess()} — 2xx（请求成功）</li>
 *   <li>{@link #isRedirect()} — 3xx（重定向）</li>
 *   <li>{@link #isClientError()} — 4xx（客户端错误）</li>
 *   <li>{@link #isServerError()} — 5xx（服务端错误）</li>
 * </ul>
 *
 * @author linjpxc
 */
public interface Response extends HttpResponse {

    /**
     * 获取已缓冲的响应体。可多次调用，内容不会消耗。
     */
    @Nonnull
    ResponseBody getBody();

    /** 状态码在 200-299 范围内。 */
    default boolean isSuccess() {
        final int code = this.getStatusCode();
        return code >= 200 && code < 300;
    }

    /** 状态码在 300-399 范围内。 */
    default boolean isRedirect() {
        final int code = this.getStatusCode();
        return code >= 300 && code < 400;
    }

    /** 状态码在 400-499 范围内（如 404 Not Found、401 Unauthorized）。 */
    default boolean isClientError() {
        final int code = this.getStatusCode();
        return code >= 400 && code < 500;
    }

    /** 状态码在 500-599 范围内（如 500 Internal Server Error）。 */
    default boolean isServerError() {
        final int code = this.getStatusCode();
        return code >= 500 && code < 600;
    }
}
