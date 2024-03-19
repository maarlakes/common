package cn.maarlakes.common.http;

/**
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
