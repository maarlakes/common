package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;
import org.apache.http.client.protocol.HttpClientContext;

import java.net.Proxy;

/**
 * SPI for proxy authentication in Apache HttpClient backend.
 *
 * @author linjpxc
 */
public interface Apache4ProxyAuthenticator {

    boolean supported(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);

    void authenticate(@Nonnull HttpClientContext context, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);
}
