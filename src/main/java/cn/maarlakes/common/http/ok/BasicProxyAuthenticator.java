package cn.maarlakes.common.http.ok;

import cn.maarlakes.common.http.HttpHeaderNames;
import cn.maarlakes.common.http.proxy.BasicAuthentication;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;
import okhttp3.Authenticator;
import org.jetbrains.annotations.Nullable;

import java.net.Proxy;

@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class BasicProxyAuthenticator implements ProxyAuthenticator {

    @Nullable
    @Override
    public Authenticator authenticate(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        if (authentication instanceof BasicAuthentication) {
            final BasicAuthentication auth = (BasicAuthentication) authentication;
            return (route, response) -> {
                final String credential = okhttp3.Credentials.basic(auth.getUsername(), auth.getPassword());
                return response.request().newBuilder().header(HttpHeaderNames.PROXY_AUTHORIZATION, credential).build();
            };
        }
        return null;
    }
}
