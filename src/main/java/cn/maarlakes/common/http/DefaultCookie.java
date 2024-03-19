package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @author linjpxc
 */
final class DefaultCookie implements Cookie {
    private static final long serialVersionUID = 5568565153844968132L;

    private final String name;
    private final String value;
    private final String domain;
    private final String path;
    private final long maxAge;
    private final boolean isSecure;
    private final boolean isHttpOnly;
    private final SameSite sameSite;
    private final Integer version;
    private final LocalDateTime expires;

    public DefaultCookie(String name, String value, String domain, String path, long maxAge, boolean isSecure, boolean isHttpOnly, SameSite sameSite, Integer version, LocalDateTime expires) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.maxAge = maxAge;
        this.isSecure = isSecure;
        this.isHttpOnly = isHttpOnly;
        this.sameSite = sameSite;
        this.version = version;
        this.expires = expires;
    }

    @Nonnull
    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String value() {
        return this.value;
    }

    @Override
    public String domain() {
        return this.domain;
    }

    @Override
    public String path() {
        return this.path;
    }

    @Override
    public long maxAge() {
        return this.maxAge;
    }

    @Override
    public boolean isSecure() {
        return this.isSecure;
    }

    @Override
    public boolean isHttpOnly() {
        return this.isHttpOnly;
    }

    @Override
    public SameSite sameSite() {
        return this.sameSite;
    }

    @Override
    public Integer version() {
        return this.version;
    }

    @Override
    public LocalDateTime expires() {
        return this.expires;
    }

    @Override
    public int compareTo(@Nullable Cookie other) {
        if (other == null) {
            return 1;
        }
        int index = this.name.compareTo(other.name());
        if (index != 0) {
            return index;
        }
        if (this.domain == null) {
            if (other.domain() != null) {
                return -1;
            }
        } else if (other.domain() == null) {
            return 1;
        } else {
            index = this.domain.compareToIgnoreCase(other.domain());
            if (index != 0) {
                return index;
            }
        }

        if (this.path == null) {
            if (other.path() == null) {
                return 0;
            }
            return -1;
        }
        if (other.path() == null) {
            return 1;
        }
        return this.path.compareTo(other.path());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof Cookie) {
            final Cookie that = (Cookie) obj;
            if (!this.name.equals(that.name())) {
                return false;
            }
            if (this.domain == null) {
                if (that.domain() != null) {
                    return false;
                }
            } else if (that.domain() == null) {
                return false;
            } else if (!this.domain.equalsIgnoreCase(that.domain())) {
                return false;
            }

            if (this.path == null) {
                return that.path() == null;
            }
            return this.path.equals(that.path());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder()
                .append(this.name).append("=").append(this.value);
        if (this.domain != null) {
            builder.append(", Domain=").append(this.domain);
        }
        if (this.path != null) {
            builder.append(", Path=").append(this.path);
        }
        if (this.maxAge > 0L) {
            builder.append(", MaxAge=").append(this.maxAge).append("s");
        }
        if (this.isSecure) {
            builder.append(", secure");
        }
        if (this.isHttpOnly) {
            builder.append(", HttpOnly");
        }
        if (this.sameSite != null) {
            builder.append(", SameSite=").append(this.sameSite);
        }
        return builder.toString();
    }
}
