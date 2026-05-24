package cn.maarlakes.common.http.async;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;
import org.asynchttpclient.proxy.ProxyServer;

import java.net.Proxy;

/**
 * AsyncHttpClient (Netty) 的代理认证 SPI 接口。
 *
 * <p>为 AsyncHttpClient 后端提供代理认证能力。实现类根据认证凭证
 * 配置 {@link ProxyServer.Builder} 的 Realm 信息，返回配置好的 Builder。</p>
 *
 * @author linjpxc
 */
public interface ProxyAuthenticator {

    /**
     * 根据认证凭证配置并返回 AsyncHttpClient 的代理服务器构建器。
     *
     * @param proxy          代理配置
     * @param authentication 认证凭证
     * @return 配置了认证信息的 ProxyServer.Builder，不支持该认证类型时返回 {@code null}
     */
    ProxyServer.Builder authenticate(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);
}
