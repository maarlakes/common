package cn.maarlakes.common.http.ok;

import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import okhttp3.Authenticator;

import java.net.Proxy;

/**
 * OkHttp 的代理认证 SPI 接口。
 *
 * <p>为 OkHttp 后端提供代理认证能力。实现类返回 OkHttp 原生的 {@link Authenticator}，
 * 由 OkHttp 框架在收到 407 响应时自动调用，完成认证头的设置。</p>
 *
 * @author linjpxc
 */
public interface ProxyAuthenticator {

    /**
     * 根据认证凭证创建 OkHttp 的 {@link Authenticator} 实例。
     *
     * @param proxy          代理配置
     * @param authentication 认证凭证
     * @return 对应的 OkHttp Authenticator，不支持该认证类型时返回 {@code null}
     */
    @Nullable
    Authenticator authenticate(@Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication);
}
