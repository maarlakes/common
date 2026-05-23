package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface Response extends HttpResponse {

    @Nonnull
    ResponseBody getBody();

    default boolean isSuccess() {
        final int code = this.getStatusCode();
        return code >= 200 && code < 300;
    }

    default boolean isRedirect() {
        final int code = this.getStatusCode();
        return code >= 300 && code < 400;
    }

    default boolean isClientError() {
        final int code = this.getStatusCode();
        return code >= 400 && code < 500;
    }

    default boolean isServerError() {
        final int code = this.getStatusCode();
        return code >= 500 && code < 600;
    }
}
