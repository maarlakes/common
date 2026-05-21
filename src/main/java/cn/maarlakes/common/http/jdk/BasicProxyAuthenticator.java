package cn.maarlakes.common.http.jdk;

import cn.maarlakes.common.http.HttpHeaderNames;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.http.proxy.UsernamePasswordProxyAuthentication;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.util.Base64;

@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class BasicProxyAuthenticator implements ProxyAuthenticator {

    @Override
    public boolean supported(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        return authentication instanceof UsernamePasswordProxyAuthentication;
    }

    @Override
    public void authenticate(@Nonnull HttpURLConnection connection, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        if (authentication instanceof UsernamePasswordProxyAuthentication) {
            final UsernamePasswordProxyAuthentication usernamePasswordProxyAuthentication = (UsernamePasswordProxyAuthentication) authentication;
            connection.setRequestProperty(HttpHeaderNames.PROXY_AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((usernamePasswordProxyAuthentication.getUsername() + ":" + usernamePasswordProxyAuthentication.getPassword()).getBytes()));
        } else {
            throw new IllegalArgumentException("ProxyAuthentication must be UsernamePasswordProxyAuthentication");
        }
    }
}
