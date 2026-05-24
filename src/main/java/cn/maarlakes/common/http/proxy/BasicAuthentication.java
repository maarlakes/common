package cn.maarlakes.common.http.proxy;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * HTTP Basic 认证的代理凭证，持有用户名和密码。
 *
 * <p>用于 HTTP 代理服务器的 Basic 认证方案。各后端的 BasicProxyAuthenticator
 * 会读取此对象中的用户名和密码，构建对应的认证头或凭证提供者。
 * 也可用于 Digest 认证场景（Digest 认证同样需要用户名和密码作为输入）。</p>
 *
 * @author linjpxc
 */
public class BasicAuthentication implements ProxyAuthentication {
    private static final long serialVersionUID = -5697995558694700806L;

    private final String username;
    private final String password;

    public BasicAuthentication(@Nonnull String username, @Nonnull String password) {
        this.username = username;
        this.password = password;
    }

    @Nonnull
    public String getUsername() {
        return username;
    }

    @Nonnull
    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BasicAuthentication) {
            final BasicAuthentication that = (BasicAuthentication) o;
            return Objects.equals(username, that.username) && Objects.equals(password, that.password);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

    @Override
    public String toString() {
        return this.username;
    }
}
