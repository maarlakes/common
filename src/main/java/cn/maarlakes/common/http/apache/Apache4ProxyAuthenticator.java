package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;
import org.apache.http.client.protocol.HttpClientContext;

import java.net.Proxy;

/**
 * Apache HttpClient 4 的代理认证 SPI 接口。
 *
 * <p>为 Apache HttpClient 4 后端提供代理认证能力。接口设计与 Apache 5 版本
 * ({@link ProxyAuthenticator}) 保持一致，但使用 Apache 4 的 {@link HttpClientContext}。
 * 实现类通过 SPI 加载。</p>
 *
 * @author linjpxc
 */
public interface Apache4ProxyAuthenticator {

    /**
     * 判断是否支持给定代理和认证类型的组合。
     */
    boolean supported(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);

    /**
     * 在 Apache HttpClient 4 的请求上下文中配置代理认证凭证。
     */
    void authenticate(@Nonnull HttpClientContext context, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);
}
