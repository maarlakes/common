package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

/**
 * @author linjpxc
 */
public interface Response extends Closeable {

    int getStatusCode();

    String getStatusText();

    @Nonnull
    ResponseBody getBody();

    URI getUri();

    @Nonnull
    HttpHeaders getHeaders();

    @Nonnull
    List<? extends Cookie> getCookies();

    SocketAddress getRemoteAddress();

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

    @Override
    default void close() throws IOException {
    }
}
