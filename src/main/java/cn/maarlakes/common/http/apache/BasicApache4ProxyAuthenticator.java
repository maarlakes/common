package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.http.proxy.UsernamePasswordProxyAuthentication;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.BasicCredentialsProvider;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * Basic scheme proxy authenticator for Apache HttpClient backend.
 *
 * @author linjpxc
 */
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class BasicApache4ProxyAuthenticator implements Apache4ProxyAuthenticator {

    @Override
    public boolean supported(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        return authentication instanceof UsernamePasswordProxyAuthentication;
    }

    @Override
    public void authenticate(@Nonnull HttpClientContext context, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        if (authentication instanceof UsernamePasswordProxyAuthentication) {
            final UsernamePasswordProxyAuthentication auth = (UsernamePasswordProxyAuthentication) authentication;
            final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            final InetSocketAddress address = (InetSocketAddress) proxy.address();
            credentialsProvider.setCredentials(
                    new AuthScope(address.getAddress().getHostName(), address.getPort()),
                    new UsernamePasswordCredentials(auth.getUsername(), auth.getPassword())
            );
            context.setCredentialsProvider(credentialsProvider);
        } else {
            throw new IllegalArgumentException("Unsupported authentication type: " + authentication.getClass().getName());
        }
    }
}
