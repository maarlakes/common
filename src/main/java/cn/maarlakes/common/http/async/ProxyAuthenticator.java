package cn.maarlakes.common.http.async;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;
import org.asynchttpclient.proxy.ProxyServer;

import java.net.Proxy;

public interface ProxyAuthenticator {

    ProxyServer.Builder authenticate(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);
}
