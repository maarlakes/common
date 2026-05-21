package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.http.proxy.UsernamePasswordProxyAuthentication;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.protocol.HttpClientContext;

import java.net.InetSocketAddress;
import java.net.Proxy;

@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class BasicProxyAuthenticator implements ProxyAuthenticator {
    @Override
    public boolean supported(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        return authentication instanceof UsernamePasswordProxyAuthentication;
    }

    @Override
    public void authenticate(@Nonnull HttpClientContext context, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        if (authentication instanceof UsernamePasswordProxyAuthentication) {
            final UsernamePasswordProxyAuthentication auth = (UsernamePasswordProxyAuthentication) authentication;
            final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            final InetSocketAddress address = (InetSocketAddress) proxy.address();
            credentialsProvider.setCredentials(
                    new org.apache.hc.client5.http.auth.AuthScope(address.getAddress().getHostName(), address.getPort()),
                    new org.apache.hc.client5.http.auth.UsernamePasswordCredentials(auth.getUsername(), auth.getPassword().toCharArray())
            );
            context.setCredentialsProvider(credentialsProvider);
        } else {
            throw new IllegalArgumentException("Unsupported authentication type: " + authentication.getClass().getName());
        }
    }
}
