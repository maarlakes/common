package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;
import org.apache.hc.client5.http.protocol.HttpClientContext;

import java.net.Proxy;

/**
 * Apache HttpClient 5 的代理认证 SPI 接口。
 *
 * <p>为 Apache HttpClient 5 后端提供代理认证能力。实现类通过 SPI 加载，
 * 根据代理类型和认证凭证类型判断是否支持，并在 {@link HttpClientContext} 中设置认证信息。</p>
 *
 * @author linjpxc
 */
public interface ProxyAuthenticator {

    /**
     * 判断是否支持给定代理和认证类型的组合。
     *
     * @param proxy          代理配置
     * @param authentication 认证凭证
     * @return 如果本认证器能处理该组合则返回 {@code true}
     */
    boolean supported(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);

    /**
     * 在 Apache HttpClient 5 的请求上下文中配置代理认证凭证。
     *
     * @param context        Apache HttpClient 请求上下文
     * @param proxy          代理配置
     * @param authentication 认证凭证
     */
    void authenticate(@Nonnull HttpClientContext context, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);
}
