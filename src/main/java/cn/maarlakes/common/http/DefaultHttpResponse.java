package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.net.SocketAddress;
import java.net.URI;
import java.util.List;

/**
 * @author linjpxc
 */
public class DefaultHttpResponse implements HttpResponse {

    private final int statusCode;
    private final String statusText;
    private final HttpHeaders headers;
    private final URI uri;
    private final List<? extends Cookie> cookies;
    private final SocketAddress remoteAddress;

    public DefaultHttpResponse(int statusCode, String statusText, @Nonnull HttpHeaders headers, URI uri,
                               @Nonnull List<? extends Cookie> cookies, SocketAddress remoteAddress) {
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.headers = headers;
        this.uri = uri;
        this.cookies = cookies;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public int getStatusCode() {
        return this.statusCode;
    }

    @Override
    public String getStatusText() {
        return this.statusText;
    }

    @Nonnull
    @Override
    public HttpHeaders getHeaders() {
        return this.headers;
    }

    @Override
    public URI getUri() {
        return this.uri;
    }

    @Nonnull
    @Override
    public List<? extends Cookie> getCookies() {
        return this.cookies;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return this.remoteAddress;
    }
}
