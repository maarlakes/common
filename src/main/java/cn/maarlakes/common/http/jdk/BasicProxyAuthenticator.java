package cn.maarlakes.common.http.jdk;

import cn.maarlakes.common.http.HttpHeaderNames;
import cn.maarlakes.common.http.proxy.BasicAuthentication;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.util.Base64;

/**
 * JDK HttpURLConnection 后端的 Basic 代理认证实现。
 *
 * <p>将用户名和密码以 Base64 编码后直接设置到请求的 Proxy-Authorization 头中。
 * 由于 JDK 的 HttpURLConnection 不支持自动代理认证重试，
 * 此认证器在请求发送前预置认证信息。</p>
 *
 * @author linjpxc
 */
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class BasicProxyAuthenticator implements ProxyAuthenticator {

    @Override
    public boolean supported(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        return authentication instanceof BasicAuthentication;
    }

    @Override
    public void authenticate(@Nonnull HttpURLConnection connection, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        if (authentication instanceof BasicAuthentication) {
            final BasicAuthentication basicAuthentication = (BasicAuthentication) authentication;
            connection.setRequestProperty(HttpHeaderNames.PROXY_AUTHORIZATION, "Basic " + Base64.getEncoder().encodeToString((basicAuthentication.getUsername() + ":" + basicAuthentication.getPassword()).getBytes()));
        } else {
            throw new IllegalArgumentException("ProxyAuthentication must be UsernamePasswordProxyAuthentication");
        }
    }
}
