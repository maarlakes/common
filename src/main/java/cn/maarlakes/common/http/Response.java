package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author linjpxc
 */
public interface Response {

    int getStatusCode();

    String getStatusText();

    default String getBody() {
        return this.getBody(StandardCharsets.UTF_8);
    }

    String getBody(@Nonnull Charset charset);

    InputStream getBodyAsStream();

    byte[] getBodyAsBytes();

    URI getUri();

    String getContentType();

    @Nonnull
    HttpHeaders getHeaders();

    @Nonnull
    List<? extends Cookie> getCookies();

    SocketAddress getRemoteAddress();
}
