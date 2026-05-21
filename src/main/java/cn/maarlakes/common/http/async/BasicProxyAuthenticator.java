package cn.maarlakes.common.http.async;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.http.proxy.UsernamePasswordProxyAuthentication;
import jakarta.annotation.Nonnull;
import org.asynchttpclient.proxy.ProxyServer;

import java.net.InetSocketAddress;
import java.net.Proxy;

public class BasicProxyAuthenticator implements ProxyAuthenticator {

    @Override
    public ProxyServer.Builder authenticate(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        if (authentication instanceof UsernamePasswordProxyAuthentication) {
            final UsernamePasswordProxyAuthentication auth = (UsernamePasswordProxyAuthentication) authentication;
            final InetSocketAddress address = (InetSocketAddress) proxy.address();
            final ProxyServer.Builder builder = new ProxyServer.Builder(address.getHostName(), address.getPort());
            builder.setRealm(
                    new org.asynchttpclient.Realm.Builder(auth.getUsername(), auth.getPassword())
                            .setScheme(org.asynchttpclient.Realm.AuthScheme.BASIC)
                            .build()
            );
            return builder;
        }
        return null;
    }
}
