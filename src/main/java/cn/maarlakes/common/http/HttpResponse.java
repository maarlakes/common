package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

public interface HttpResponse {

    int getStatusCode();

    String getStatusText();

    URI getUri();

    @Nonnull
    HttpHeaders getHeaders();

    @Nonnull
    List<? extends Cookie> getCookies();

    SocketAddress getRemoteAddress();
}
