package cn.maarlakes.common.http.jdk;

import cn.maarlakes.common.http.Header;
import cn.maarlakes.common.http.HttpHeaderNames;
import cn.maarlakes.common.http.Response;
import cn.maarlakes.common.http.proxy.DigestAuthentication;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URISyntaxException;

@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class DigestProxyAuthenticator implements ProxyAuthenticator {

    @Override
    public boolean supported(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        return authentication instanceof DigestAuthentication;
    }

    @Override
    public boolean authenticate(@Nonnull HttpURLConnection connection, @Nonnull Response response, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        if (authentication instanceof DigestAuthentication) {
            final Header proxyAuthenticateHeader = response.getHeaders().getHeader(HttpHeaderNames.PROXY_AUTHENTICATE);
            if (proxyAuthenticateHeader == null) {
                return false;
            }
            final String proxyAuthenticate = proxyAuthenticateHeader.get();
            if (proxyAuthenticate == null || !proxyAuthenticate.toLowerCase().startsWith("digest")) {
                return false;
            }

            final DigestAuthentication auth = (DigestAuthentication) authentication;
            try {
                connection.setRequestProperty(HttpHeaderNames.PROXY_AUTHORIZATION, auth.toAuthorization(proxyAuthenticate, connection.getRequestMethod(), connection.getURL().toURI().toString()));
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            return true;
        } else {
            throw new IllegalArgumentException("ProxyAuthentication must be DigestAuthentication");
        }
    }
}
