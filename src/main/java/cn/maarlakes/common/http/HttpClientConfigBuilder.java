package cn.maarlakes.common.http;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.net.ssl.SSLContext;
import java.net.Proxy;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * @author linjpxc
 */
class HttpClientConfigBuilder implements HttpClientConfig.Builder {

    private Boolean redirectsEnabled;
    private Duration requestTimeout;
    private Duration connectTimeout;
    private Duration responseTimeout;
    private Proxy proxy;
    private ProxyAuthentication proxyAuthentication;
    private Integer maxRedirects;
    private SSLContext sslContext;
    private Executor executor;

    @Nonnull
    @Override
    public HttpClientConfig.Builder redirectsEnabled(Boolean enabled) {
        this.redirectsEnabled = enabled;
        return this;
    }

    @Nonnull
    @Override
    public HttpClientConfig.Builder requestTimeout(Duration timeout) {
        this.requestTimeout = timeout;
        return this;
    }

    @Nonnull
    @Override
    public HttpClientConfig.Builder connectTimeout(Duration timeout) {
        this.connectTimeout = timeout;
        return this;
    }

    @Nonnull
    @Override
    public HttpClientConfig.Builder responseTimeout(Duration timeout) {
        this.responseTimeout = timeout;
        return this;
    }

    @Nonnull
    @Override
    public HttpClientConfig.Builder proxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    @Nonnull
    @Override
    public HttpClientConfig.Builder proxyAuthentication(ProxyAuthentication proxyAuthentication) {
        this.proxyAuthentication = proxyAuthentication;
        return this;
    }

    @Nonnull
    @Override
    public HttpClientConfig.Builder maxRedirects(Integer maxRedirects) {
        this.maxRedirects = maxRedirects;
        return this;
    }

    @Nonnull
    @Override
    public HttpClientConfig.Builder sslContext(@Nullable SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    @Nonnull
    @Override
    public HttpClientConfig.Builder executor(@Nullable Executor executor) {
        this.executor = executor;
        return this;
    }

    @Nonnull
    @Override
    public HttpClientConfig build() {
        return new DefaultHttpClientConfig(
                this.redirectsEnabled, this.requestTimeout, this.connectTimeout,
                this.responseTimeout, this.proxy, this.proxyAuthentication,
                this.maxRedirects, this.sslContext, this.executor
        );
    }

    private static class DefaultHttpClientConfig implements HttpClientConfig {
        private static final long serialVersionUID = 6724752825582438174L;

        private final Boolean redirectsEnabled;
        private final Duration requestTimeout;
        private final Duration connectTimeout;
        private final Duration responseTimeout;
        private final Proxy proxy;
        private final ProxyAuthentication proxyAuthentication;
        private final Integer maxRedirects;
        private final SSLContext sslContext;
        private final Executor executor;

        private DefaultHttpClientConfig(Boolean redirectsEnabled, Duration requestTimeout,
                                        Duration connectTimeout, Duration responseTimeout,
                                        Proxy proxy, ProxyAuthentication proxyAuthentication,
                                        Integer maxRedirects, SSLContext sslContext,
                                        Executor executor) {
            this.redirectsEnabled = redirectsEnabled;
            this.requestTimeout = requestTimeout;
            this.connectTimeout = connectTimeout;
            this.responseTimeout = responseTimeout;
            this.proxy = proxy;
            this.proxyAuthentication = proxyAuthentication;
            this.maxRedirects = maxRedirects;
            this.sslContext = sslContext;
            this.executor = executor;
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
            return proxyAuthentication;
        }

        @Override
        public Integer getMaxRedirects() {
            return maxRedirects;
        }

        @Override
        public SSLContext getSslContext() {
            return sslContext;
        }

        @Override
        public Executor getExecutor() {
            return executor;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof HttpClientConfig) {
                final HttpClientConfig that = (HttpClientConfig) obj;
                return Objects.equals(this.isRedirectsEnabled(), that.isRedirectsEnabled())
                        && Objects.equals(this.getRequestTimeout(), that.getRequestTimeout())
                        && Objects.equals(this.getConnectTimeout(), that.getConnectTimeout())
                        && Objects.equals(this.getResponseTimeout(), that.getResponseTimeout())
                        && Objects.equals(this.getProxy(), that.getProxy())
                        && Objects.equals(this.proxyAuthentication, that.getProxyAuthentication())
                        && Objects.equals(this.getMaxRedirects(), that.getMaxRedirects())
                        && Objects.equals(this.sslContext, that.getSslContext())
                        && Objects.equals(this.executor, that.getExecutor());
            }
            return false;
        }

        @Override
        public String toString() {
            return "redirectsEnabled=" + isRedirectsEnabled()
                    + ", requestTimeout=" + requestTimeout
                    + ", connectTimeout=" + connectTimeout
                    + ", responseTimeout=" + responseTimeout
                    + ", proxy=" + proxy
                    + ", maxRedirects=" + maxRedirects
                    + ", sslContext=" + (sslContext != null)
                    + ", executor=" + executor;
        }
    }
}
