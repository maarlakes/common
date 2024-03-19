package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
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
