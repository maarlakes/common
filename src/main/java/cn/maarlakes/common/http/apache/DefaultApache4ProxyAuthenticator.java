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
 * Apache HttpClient 4 后端的代理认证实现，支持 Basic 和 Digest 两种认证方案。
 *
 * <p>通过在 {@link BasicCredentialsProvider} 中设置 {@link UsernamePasswordCredentials}，
 * 让 Apache HttpClient 4 自动处理代理认证流程。逻辑与 Apache 5 版本
 * ({@link DefaultProxyAuthenticator}) 一致，但使用 Apache 4 的 API。</p>
 *
 * @author linjpxc
 */
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class DefaultApache4ProxyAuthenticator implements Apache4ProxyAuthenticator {

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
