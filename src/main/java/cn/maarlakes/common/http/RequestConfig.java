package cn.maarlakes.common.http;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.net.ssl.SSLContext;
import java.io.Serializable;
import java.net.Proxy;
import java.time.Duration;

/**
 * @author linjpxc
 */
public interface RequestConfig extends Serializable {

    Boolean isRedirectsEnabled();

    Duration getRequestTimeout();

    Duration getConnectTimeout();

    Duration getResponseTimeout();

    Proxy getProxy();

    Integer getMaxRedirects();

    ProxyAuthentication getProxyAuthentication();

    @Nullable
    SSLContext getSslContext();

    @Nonnull
    static Builder builder() {
        return new RequestConfigBuilder();
    }

    interface Builder {

        @Nonnull
        Builder redirectsEnabled(Boolean enabled);

        @Nonnull
        Builder requestTimeout(Duration timeout);

        @Nonnull
        Builder connectTimeout(Duration timeout);

        @Nonnull
        Builder responseTimeout(Duration timeout);

        @Nonnull
        Builder proxy(Proxy proxy);

        @Nonnull
        Builder proxyAuthentication(ProxyAuthentication proxyAuthentication);

        @Nonnull
        Builder maxRedirects(Integer maxRedirects);

        @Nonnull
        Builder sslContext(SSLContext sslContext);

        @Nonnull
        RequestConfig build();
    }
}
