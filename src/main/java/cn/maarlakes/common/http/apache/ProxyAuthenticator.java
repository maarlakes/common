package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;
import org.apache.hc.client5.http.protocol.HttpClientContext;

import java.net.Proxy;

public interface ProxyAuthenticator {

    boolean supported(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);

    void authenticate(@Nonnull HttpClientContext context, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);
}
