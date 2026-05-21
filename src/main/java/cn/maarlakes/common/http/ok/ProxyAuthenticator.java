package cn.maarlakes.common.http.ok;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import okhttp3.Authenticator;

import java.net.Proxy;

/**
 * SPI for proxy authentication in OkHttp backend.
 *
 * @author linjpxc
 */
public interface ProxyAuthenticator {

    @Nullable
    Authenticator authenticate(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);
}
