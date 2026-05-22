package cn.maarlakes.common.http;

import jakarta.annotation.Nullable;

/**
 * @author linjpxc
 */
public final class RequestConfigs {

    private RequestConfigs() {
    }

    @Nullable
    public static RequestConfig merge(@Nullable RequestConfig defaultConfig, @Nullable RequestConfig requestConfig) {
        if (requestConfig == null) {
            return defaultConfig;
        }
        if (defaultConfig == null) {
            return requestConfig;
        }
        return RequestConfig.builder()
                .redirectsEnabled(requestConfig.isRedirectsEnabled() != null
                        ? requestConfig.isRedirectsEnabled() : defaultConfig.isRedirectsEnabled())
                .requestTimeout(requestConfig.getRequestTimeout() != null
                        ? requestConfig.getRequestTimeout() : defaultConfig.getRequestTimeout())
                .connectTimeout(requestConfig.getConnectTimeout() != null
                        ? requestConfig.getConnectTimeout() : defaultConfig.getConnectTimeout())
                .responseTimeout(requestConfig.getResponseTimeout() != null
                        ? requestConfig.getResponseTimeout() : defaultConfig.getResponseTimeout())
                .proxy(requestConfig.getProxy() != null
                        ? requestConfig.getProxy() : defaultConfig.getProxy())
                .proxyAuthentication(requestConfig.getProxyAuthentication() != null
                        ? requestConfig.getProxyAuthentication() : defaultConfig.getProxyAuthentication())
                .maxRedirects(requestConfig.getMaxRedirects() != null
                        ? requestConfig.getMaxRedirects() : defaultConfig.getMaxRedirects())
                .build();
    }
}
