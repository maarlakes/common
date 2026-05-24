package cn.maarlakes.common.http.ok;

import cn.maarlakes.common.http.HttpHeaderNames;
import cn.maarlakes.common.http.proxy.DigestAuthentication;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import okhttp3.Authenticator;

import java.net.Proxy;

/**
 * OkHttp 后端的 Digest 代理认证实现。
 *
 * <p>返回一个 OkHttp {@link Authenticator}，在收到 407 响应时读取
 * Proxy-Authenticate 头，调用 {@link DigestAuthentication#toAuthorization}
 * 计算 Digest 认证响应值，并添加 Proxy-Authorization 头后重试请求。</p>
 *
 * @author linjpxc
 */
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class DigestProxyAuthenticator implements ProxyAuthenticator {

    @Nullable
    @Override
    public Authenticator authenticate(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        if (authentication instanceof DigestAuthentication) {
            final DigestAuthentication auth = (DigestAuthentication) authentication;
            return (route, response) -> {
                final String proxyAuthenticate = response.header(HttpHeaderNames.PROXY_AUTHENTICATE);
                if (proxyAuthenticate == null || !proxyAuthenticate.toLowerCase().startsWith("digest")) {
                    return null;
                }
                final String authorization = auth.toAuthorization(proxyAuthenticate, response.request().method(), response.request().url().uri().toString());
                if (authorization != null && !authorization.isEmpty()) {
                    return response.request().newBuilder()
                            .header(HttpHeaderNames.PROXY_AUTHORIZATION, authorization)
                            .build();
                }
                return null;
            };
        }
        return null;
    }
}
