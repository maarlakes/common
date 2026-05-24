package cn.maarlakes.common.http.jdk;

import cn.maarlakes.common.http.Response;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;

import java.net.HttpURLConnection;
import java.net.Proxy;

/**
 * JDK HttpURLConnection 的代理认证 SPI 接口。
 *
 * <p>为 JDK 原生 HTTP 客户端后端提供代理认证能力。由于 JDK 的 HttpURLConnection
 * 不支持自动的代理认证重试，因此提供两种认证方法：预置认证和响应后认证。
 * Digest 认证需要读取服务端的 407 响应头，因此使用第二个方法。</p>
 *
 * @author linjpxc
 */
public interface ProxyAuthenticator {

    /**
     * 判断是否支持给定代理和认证类型的组合。
     */
    boolean supported(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);

    /**
     * 在发送请求前预置代理认证信息（如 Basic 认证）。
     *
     * @param connection     HTTP 连接
     * @param proxy          代理配置
     * @param authentication 认证凭证
     */
    default void authenticate(@Nonnull HttpURLConnection connection, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
    }

    /**
     * 收到 407 响应后执行代理认证（如 Digest 认证需要读取服务端挑战头）。
     *
     * @param connection     HTTP 连接
     * @param response       服务端的 407 响应
     * @param proxy          代理配置
     * @param authentication 认证凭证
     * @return 如果成功设置了认证头则返回 {@code true}
     */
    default boolean authenticate(@Nonnull HttpURLConnection connection, @Nonnull Response response, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        return false;
    }
}
