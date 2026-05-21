package cn.maarlakes.common.http.jdk;

import cn.maarlakes.common.http.Response;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;

import java.net.HttpURLConnection;
import java.net.Proxy;

public interface ProxyAuthenticator {

    boolean supported(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);

    default void authenticate(@Nonnull HttpURLConnection connection, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
    }

    default boolean authenticate(@Nonnull HttpURLConnection connection, @Nonnull Response response, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        return false;
    }
}
