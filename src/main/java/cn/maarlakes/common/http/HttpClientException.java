package cn.maarlakes.common.http;

/**
 * HTTP 客户端框架的统一运行时异常。
 *
 * <p>用于封装底层 HTTP 库抛出的各类受检异常，使调用方无需处理 {@code IOException} 等受检异常。
 * 作为 {@link RuntimeException} 的子类，可以自然地在 {@link java.util.concurrent.CompletableFuture} 的异步链中传播。
 *
 * @author linjpxc
 */
public class HttpClientException extends RuntimeException {
    private static final long serialVersionUID = -1303880329584786292L;

    public HttpClientException() {
    }

    public HttpClientException(String message) {
        super(message);
    }

    public HttpClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpClientException(Throwable cause) {
        super(cause);
    }

    public HttpClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
