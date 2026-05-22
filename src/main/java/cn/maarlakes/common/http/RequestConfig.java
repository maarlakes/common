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

    Boolean isRedirectsEnabled();

    Duration getRequestTimeout();

    Duration getConnectTimeout();

    Duration getResponseTimeout();

    Proxy getProxy();

    Integer getMaxRedirects();

    ProxyAuthentication getProxyAuthentication();

    RequestConfig DEFAULT = new RequestConfigBuilder().build();

    @Nonnull
    static Builder builder() {
        return new RequestConfigBuilder();
    }

    interface Builder extends BaseRequestConfigBuilder<Builder, RequestConfig> {
    }
}
