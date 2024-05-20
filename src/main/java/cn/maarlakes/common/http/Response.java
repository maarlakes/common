package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

/**
 * @author linjpxc
 */
public interface Response {

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
}
