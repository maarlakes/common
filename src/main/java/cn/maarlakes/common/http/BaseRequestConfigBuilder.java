package cn.maarlakes.common.http;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;

import java.net.Proxy;
import java.time.Duration;

interface BaseRequestConfigBuilder<B extends BaseRequestConfigBuilder<B, C>, C extends RequestConfig> {

    @Nonnull
    B redirectsEnabled(Boolean enabled);

    @Nonnull
    B requestTimeout(Duration timeout);

    @Nonnull
    B connectTimeout(Duration timeout);

    @Nonnull
    B responseTimeout(Duration timeout);

    @Nonnull
    B proxy(Proxy proxy);

    @Nonnull
    B proxyAuthentication(ProxyAuthentication proxyAuthentication);

    @Nonnull
    B maxRedirects(Integer maxRedirects);

    @Nonnull
    C build();
}
