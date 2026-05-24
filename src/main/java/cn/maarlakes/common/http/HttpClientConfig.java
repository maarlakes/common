package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.net.ssl.SSLContext;
import java.util.concurrent.Executor;

/**
 * HTTP 客户端级别的配置，扩展 {@link RequestConfig} 添加 SSL 和执行器设置。
 *
 * <p>与 {@link RequestConfig} 的关系：{@code RequestConfig} 是请求级别的（可每次调用不同），
 * 而 {@code HttpClientConfig} 是客户端级别的（创建时确定，影响所有通过该客户端发出的请求）。
 *
 * <p>{@link #getSslContext()} 用于 HTTPS 请求的自定义 SSL/TLS 配置（如信任自签名证书）。
 * {@link #getExecutor()} 提供异步回调执行的线程池，为 null 时由底层库自行管理。
 *
 * @author linjpxc
 */
public interface HttpClientConfig extends RequestConfig {

    /** SSL 上下文，用于自定义 HTTPS 证书验证。null 使用 JDK 默认。 */
    @Nullable
    SSLContext getSslContext();

    /** 异步回调执行器。null 由底层库自行管理线程池。 */
    Executor getExecutor();

    @Nonnull
    static Builder builder() {
        return new HttpClientConfigBuilder();
    }

    /**
     * 客户端配置构建器。
     *
     * <p>{@link #from} 方法支持从 {@link RequestConfig} 复制通用配置项，
     * 用于将请求级配置提升为客户端级配置。
     */
    interface Builder extends BaseRequestConfigBuilder<Builder, HttpClientConfig> {

        @Nonnull
        Builder sslContext(@Nullable SSLContext sslContext);

        @Nonnull
        Builder executor(@Nullable Executor executor);

        /**
         * 从 {@link RequestConfig} 复制通用配置项到当前构建器。
         */
        @Nonnull
        default Builder from(@Nonnull RequestConfig config) {
            return this.redirectsEnabled(config.isRedirectsEnabled())
                    .requestTimeout(config.getRequestTimeout())
                    .connectTimeout(config.getConnectTimeout())
                    .responseTimeout(config.getResponseTimeout())
                    .proxy(config.getProxy())
                    .proxyAuthentication(config.getProxyAuthentication())
                    .maxRedirects(config.getMaxRedirects());
        }
    }
}
