package cn.maarlakes.common.http;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.net.Proxy;
import java.time.Duration;

/**
 * @author linjpxc
 */
public interface RequestConfig extends Serializable {

    boolean isRedirectsEnabled();

    Duration getRequestTimeout();

    Duration getConnectTimeout();

    Duration getResponseTimeout();

    Proxy getProxy();

    int getMaxRedirects();

    ProxyAuthentication getProxyAuthentication();

    @Nonnull
    static Builder builder() {
        return new RequestConfigBuilder();
    }

    interface Builder {

        @Nonnull
        Builder redirectsEnabled(boolean enabled);

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
        Builder maxRedirects(int maxRedirects);

        @Nonnull
        RequestConfig build();
    }
}
