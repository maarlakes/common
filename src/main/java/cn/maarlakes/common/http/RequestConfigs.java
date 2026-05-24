package cn.maarlakes.common.http;

import jakarta.annotation.Nullable;

/**
 * 请求配置合并工具类，将客户端默认配置与请求级配置合并为最终生效的配置。
 *
 * <p>合并策略：请求级配置的非 null 值优先覆盖客户端默认配置。
 * 即调用方可以在默认配置的基础上为单次请求定制特定参数（如更短的超时）。
 *
 * @author linjpxc
 */
public final class RequestConfigs {

    private RequestConfigs() {
    }

    /**
     * 合并两个配置。请求级配置的非 null 值覆盖默认配置的对应值。
     *
     * @param defaultConfig  客户端级默认配置，可为 null
     * @param requestConfig  请求级配置，可为 null
     * @return 合并后的配置。两者都为 null 时返回 null
     */
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
