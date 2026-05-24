package cn.maarlakes.common.http.async;

import cn.maarlakes.common.http.proxy.DigestAuthentication;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;
import org.asynchttpclient.proxy.ProxyServer;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * AsyncHttpClient (Netty) 后端的 Digest 代理认证实现。
 *
 * <p>从代理地址中提取主机和端口，构建 {@link ProxyServer.Builder}，
 * 并通过 {@link org.asynchttpclient.Realm} 设置 DIGEST 认证方案。
 * AsyncHttpClient 框架会自动处理 Digest 认证的挑战-响应流程。</p>
 *
 * @author linjpxc
 */
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class DigestProxyAuthenticator implements ProxyAuthenticator {

    @Override
    public ProxyServer.Builder authenticate(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        if (authentication instanceof DigestAuthentication) {
            final DigestAuthentication auth = (DigestAuthentication) authentication;
            final InetSocketAddress address = (InetSocketAddress) proxy.address();
            final ProxyServer.Builder builder = new ProxyServer.Builder(address.getHostName(), address.getPort());
            builder.setRealm(
                    new org.asynchttpclient.Realm.Builder(auth.getUsername(), auth.getPassword())
                            .setScheme(org.asynchttpclient.Realm.AuthScheme.DIGEST)
                            .build()
            );
            return builder;
        }
        return null;
    }
}
