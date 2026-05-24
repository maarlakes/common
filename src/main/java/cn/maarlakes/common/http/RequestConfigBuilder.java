package cn.maarlakes.common.http;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;

import java.net.Proxy;
import java.time.Duration;
import java.util.Objects;

/**
 * {@link RequestConfig.Builder} 的默认实现，构建不可变的 {@link RequestConfig} 实例。
 *
 * <p>所有配置项默认为 null，表示使用底层库的默认值。
 *
 * @author linjpxc
 */
class RequestConfigBuilder implements RequestConfig.Builder {

    private Boolean redirectsEnabled;
    private Duration requestTimeout;
    private Duration connectTimeout;
    private Duration responseTimeout;
    private Proxy proxy;
    private ProxyAuthentication proxyAuthentication;
    private Integer maxRedirects;

    @Nonnull
    @Override
    public RequestConfig.Builder redirectsEnabled(Boolean enabled) {
        this.redirectsEnabled = enabled;
        return this;
    }

    @Nonnull
    @Override
    public RequestConfig.Builder requestTimeout(Duration timeout) {
        this.requestTimeout = timeout;
        return this;
    }

    @Nonnull
    @Override
    public RequestConfig.Builder connectTimeout(Duration timeout) {
        this.connectTimeout = timeout;
        return this;
    }

    @Nonnull
    @Override
    public RequestConfig.Builder responseTimeout(Duration timeout) {
        this.responseTimeout = timeout;
        return this;
    }

    @Nonnull
    @Override
    public RequestConfig.Builder proxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    @Nonnull
    @Override
    public RequestConfig.Builder proxyAuthentication(ProxyAuthentication proxyAuthentication) {
        this.proxyAuthentication = proxyAuthentication;
        return this;
    }

    @Nonnull
    @Override
    public RequestConfig.Builder maxRedirects(Integer maxRedirects) {
        this.maxRedirects = maxRedirects;
        return this;
    }

    @Nonnull
    @Override
    public RequestConfig build() {
        return new DefaultRequestConfig(this.redirectsEnabled, this.requestTimeout, this.connectTimeout, this.responseTimeout, this.proxy, this.proxyAuthentication, this.maxRedirects);
    }

    private static class DefaultRequestConfig implements RequestConfig {
        private static final long serialVersionUID = 8469433946916457328L;

        private final Boolean redirectsEnabled;
        private final Duration requestTimeout;
        private final Duration connectTimeout;
        private final Duration responseTimeout;
        private final Proxy proxy;
        private final ProxyAuthentication proxyAuthentication;
        private final Integer maxRedirects;

        private DefaultRequestConfig(Boolean redirectsEnabled, Duration requestTimeout, Duration connectTimeout, Duration responseTimeout, Proxy proxy, ProxyAuthentication proxyAuthentication, Integer maxRedirects) {
            this.redirectsEnabled = redirectsEnabled;
            this.requestTimeout = requestTimeout;
            this.connectTimeout = connectTimeout;
            this.responseTimeout = responseTimeout;
            this.proxy = proxy;
            this.proxyAuthentication = proxyAuthentication;
            this.maxRedirects = maxRedirects;
        }

        @Override
        public Boolean isRedirectsEnabled() {
            return redirectsEnabled;
        }

        @Override
        public Duration getRequestTimeout() {
            return requestTimeout;
        }

        @Override
        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        @Override
        public Duration getResponseTimeout() {
            return responseTimeout;
        }

        @Override
        public Proxy getProxy() {
            return proxy;
        }

        @Override
        public ProxyAuthentication getProxyAuthentication() {
            return this.proxyAuthentication;
        }

        @Override
        public Integer getMaxRedirects() {
            return maxRedirects;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof RequestConfig) {
                final RequestConfig that = (RequestConfig) obj;
                return Objects.equals(this.isRedirectsEnabled(), that.isRedirectsEnabled())
                        && Objects.equals(this.getRequestTimeout(), that.getRequestTimeout())
                        && Objects.equals(this.getConnectTimeout(), that.getConnectTimeout())
                        && Objects.equals(this.getResponseTimeout(), that.getResponseTimeout())
                        && Objects.equals(this.getProxy(), that.getProxy())
                        && Objects.equals(this.proxyAuthentication, that.getProxyAuthentication())
                        && Objects.equals(this.getMaxRedirects(), that.getMaxRedirects());
            }
            return false;
        }

        @Override
        public String toString() {
            return "redirectsEnabled=" + isRedirectsEnabled() + ", requestTimeout=" + requestTimeout + ", connectTimeout=" + connectTimeout + ", responseTimeout=" + responseTimeout + ", proxy=" + proxy + ", maxRedirects=" + maxRedirects;
        }
    }
}
