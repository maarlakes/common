package cn.maarlakes.common.http;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;

import java.net.Proxy;
import java.time.Duration;

/**
 * 请求配置构建器的通用基接口，定义所有配置项的 setter 方法和 {@code build()} 方法。
 *
 * <p>泛型参数允许子接口返回自身的 Builder 类型，支持流式调用链。
 * {@link RequestConfig.Builder} 和 {@link HttpClientConfig.Builder} 均继承此接口。
 *
 * @param <B> Builder 自身类型（自引用泛型）
 * @param <C> 最终构建的配置类型
 * @author linjpxc
 */
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
