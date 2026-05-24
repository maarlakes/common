package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.http.proxy.BasicAuthentication;
import cn.maarlakes.common.http.proxy.DigestAuthentication;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.protocol.HttpClientContext;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * Apache HttpClient 5 后端的代理认证实现，支持 Basic 和 Digest 两种认证方案。
 *
 * <p>通过在 {@link org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider}
 * 中设置 {@link org.apache.hc.client5.http.auth.UsernamePasswordCredentials}，
 * 让 Apache HttpClient 5 自动处理代理认证流程（包括 Basic 和 Digest）。</p>
 *
 * @author linjpxc
 */
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class DefaultProxyAuthenticator implements ProxyAuthenticator {
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
        final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        final InetSocketAddress address = (InetSocketAddress) proxy.address();
        credentialsProvider.setCredentials(
                new org.apache.hc.client5.http.auth.AuthScope(address.getAddress().getHostName(), address.getPort()),
                new org.apache.hc.client5.http.auth.UsernamePasswordCredentials(username, password.toCharArray())
        );
        context.setCredentialsProvider(credentialsProvider);
    }
}
