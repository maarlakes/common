package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.util.Objects;

/**
 * @author linjpxc
 */
class RequestConfigBuilder implements RequestConfig.Builder {

    private boolean redirectsEnabled = true;
    private Duration requestTimeout;
    private Duration connectTimeout;
    private Duration responseTimeout;

    @Nonnull
    @Override
    public RequestConfig.Builder redirectsEnabled(boolean enabled) {
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
    public RequestConfig build() {
        return new DefaultRequestConfig(this.redirectsEnabled, this.requestTimeout, this.connectTimeout, this.responseTimeout);
    }

    private static class DefaultRequestConfig implements RequestConfig {
        private static final long serialVersionUID = 8469433946916457322L;

        private final boolean redirectsEnabled;
        private final Duration requestTimeout;
        private final Duration connectTimeout;
        private final Duration responseTimeout;

        private DefaultRequestConfig(boolean redirectsEnabled, Duration requestTimeout, Duration connectTimeout, Duration responseTimeout) {
            this.redirectsEnabled = redirectsEnabled;
            this.requestTimeout = requestTimeout;
            this.connectTimeout = connectTimeout;
            this.responseTimeout = responseTimeout;
        }

        @Override
        public boolean isRedirectsEnabled() {
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
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof RequestConfig) {
                final RequestConfig that = (RequestConfig) obj;
                return this.isRedirectsEnabled() == that.isRedirectsEnabled()
                        && Objects.equals(this.getRequestTimeout(), that.getRequestTimeout())
                        && Objects.equals(this.getConnectTimeout(), that.getConnectTimeout())
                        && Objects.equals(this.getResponseTimeout(), that.getResponseTimeout());
            }
            return false;
        }

        @Override
        public String toString() {
            return "redirectsEnabled=" + isRedirectsEnabled() + ", connectionRequestTimeout=" + requestTimeout + ", connectTimeout=" + connectTimeout + ", responseTimeout=" + responseTimeout;
        }
    }
}
