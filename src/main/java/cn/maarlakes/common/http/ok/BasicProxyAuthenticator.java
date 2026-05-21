package cn.maarlakes.common.http.ok;

import cn.maarlakes.common.http.HttpHeaderNames;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.http.proxy.UsernamePasswordProxyAuthentication;
import jakarta.annotation.Nonnull;
import okhttp3.Authenticator;
import org.jetbrains.annotations.Nullable;

import java.net.Proxy;

public class BasicProxyAuthenticator implements ProxyAuthenticator {

    @Nullable
    @Override
    public Authenticator authenticate(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        if (authentication instanceof UsernamePasswordProxyAuthentication) {
            final UsernamePasswordProxyAuthentication auth = (UsernamePasswordProxyAuthentication) authentication;
            return (route, response) -> {
                final String credential = okhttp3.Credentials.basic(auth.getUsername(), auth.getPassword());
                return response.request().newBuilder().header(HttpHeaderNames.PROXY_AUTHORIZATION, credential).build();
            };
        }
        return null;
    }
}
