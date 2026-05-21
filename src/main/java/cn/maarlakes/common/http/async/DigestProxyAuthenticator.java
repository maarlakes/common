package cn.maarlakes.common.http.async;

import cn.maarlakes.common.http.proxy.DigestAuthentication;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;
import org.asynchttpclient.proxy.ProxyServer;

import java.net.InetSocketAddress;
import java.net.Proxy;

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
