package cn.maarlakes.common.http.jdk;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;

import java.net.HttpURLConnection;
import java.net.Proxy;

public interface ProxyAuthenticator {

    boolean supported(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);

    void authenticate(@Nonnull HttpURLConnection connection, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);
}
