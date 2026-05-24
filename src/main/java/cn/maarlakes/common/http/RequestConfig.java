package cn.maarlakes.common.http;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.net.Proxy;
import java.time.Duration;

/**
 * HTTP 请求级别的配置，控制单次请求的行为。
 *
 * <p>超时设计分为三层，对应 HTTP 请求的不同阶段：
 * <ul>
 *   <li>{@link #getConnectTimeout()} — 建立 TCP 连接的超时</li>
 *   <li>{@link #getRequestTimeout()} — 整个请求的超时（含连接 + 发送 + 等待首字节）</li>
 *   <li>{@link #getResponseTimeout()} — 读取完整响应体的超时</li>
 * </ul>
 *
 * <p>所有配置项均可为 null，表示使用客户端默认值或底层库默认值。
 * {@link #DEFAULT} 提供全默认的配置实例。
 *
 * <p>继承 {@link java.io.Serializable} 以支持配置的持久化和序列化传输。
 *
 * @author linjpxc
 */
public interface RequestConfig extends Serializable {

    /** 是否自动跟随重定向。null 表示使用底层库默认值。 */
    Boolean isRedirectsEnabled();

    /** 整个请求的超时时间。 */
    Duration getRequestTimeout();

    /** 建立 TCP 连接的超时时间。 */
    Duration getConnectTimeout();

    /** 读取完整响应体的超时时间。 */
    Duration getResponseTimeout();

    /** HTTP 代理配置。null 表示直连。 */
    Proxy getProxy();

    /** 最大重定向次数。null 表示使用底层库默认值。 */
    Integer getMaxRedirects();

    /** 代理认证信息（Basic/Digest）。null 表示无认证。 */
    ProxyAuthentication getProxyAuthentication();

    /** 全默认的配置实例。 */
    RequestConfig DEFAULT = new RequestConfigBuilder().build();

    /**
     * 创建请求配置构建器。
     */
    @Nonnull
    static Builder builder() {
        return new RequestConfigBuilder();
    }

    /** 请求配置构建器。 */
    interface Builder extends BaseRequestConfigBuilder<Builder, RequestConfig> {
    }
}
