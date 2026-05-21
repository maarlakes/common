package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.http.proxy.BasicAuthentication;
import cn.maarlakes.common.http.proxy.DigestAuthentication;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
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
        return authentication instanceof BasicAuthentication || authentication instanceof DigestAuthentication;
    }

    @Override
    @SuppressWarnings("DuplicatedCode")
    public void authenticate(@Nonnull HttpClientContext context, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        String username;
        String password;
        if (authentication instanceof BasicAuthentication) {
            username = ((BasicAuthentication) authentication).getUsername();
            password = ((BasicAuthentication) authentication).getPassword();
        } else if (authentication instanceof DigestAuthentication) {
            username = ((DigestAuthentication) authentication).getUsername();
            password = ((DigestAuthentication) authentication).getPassword();
        } else {
            throw new IllegalArgumentException("Unsupported authentication type: " + authentication.getClass().getName());
        }


        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        final InetSocketAddress address = (InetSocketAddress) proxy.address();
        credentialsProvider.setCredentials(
                new AuthScope(address.getAddress().getHostName(), address.getPort()),
                new UsernamePasswordCredentials(username, password)
        );
        context.setCredentialsProvider(credentialsProvider);

    }
}
