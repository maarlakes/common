package cn.maarlakes.common.http.ok;

import cn.maarlakes.common.http.HttpHeaderNames;
import cn.maarlakes.common.http.proxy.DigestAuthentication;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import okhttp3.Authenticator;

import java.net.Proxy;

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
