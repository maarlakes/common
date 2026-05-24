package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * {@link HttpMethod} 的默认实现，持有 HTTP 方法名称。
 *
 * <p>方法名称比较不区分大小写（如 "get" 和 "GET" 视为等同）。
 * 由 {@link HttpMethods} 内部创建和管理。</p>
 *
 * @author linjpxc
 */
final class DefaultHttpMethod implements HttpMethod {
    private static final long serialVersionUID = 2828763294416992489L;

    private final String name;

    DefaultHttpMethod(@Nonnull String name) {
        this.name = name;
    }

    @Nonnull
    @Override
    public String name() {
        return this.name;
    }

    @Override
    public int compareTo(@Nullable HttpMethod other) {
        if (other == null) {
            return 1;
        }
        return this.name.compareToIgnoreCase(other.name());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof HttpMethod) {
            final HttpMethod that = (HttpMethod) other;
            return this.name.equalsIgnoreCase(that.name());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return this.name;
    }
}
