package cn.maarlakes.common.http.proxy;

import jakarta.annotation.Nonnull;

import java.util.Objects;

public class UsernamePasswordProxyAuthentication implements ProxyAuthentication {
    private static final long serialVersionUID = -5697995558694700806L;

    private final String username;
    private final String password;

    public UsernamePasswordProxyAuthentication(@Nonnull String username, @Nonnull String password) {
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
        if (o instanceof UsernamePasswordProxyAuthentication) {
            final UsernamePasswordProxyAuthentication that = (UsernamePasswordProxyAuthentication) o;
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
