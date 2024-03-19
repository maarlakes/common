package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;
import reactor.util.annotation.Nullable;

import java.util.*;

/**
 * @author linjpxc
 */
public class DefaultHeader implements Header {
    private static final long serialVersionUID = -8688165886423291957L;

    private final String headerName;
    private final List<String> values;

    public DefaultHeader(@Nonnull String headerName, @Nonnull String value) {
        this(headerName, Collections.singletonList(value));
    }

    public DefaultHeader(@Nonnull String headerName, @Nonnull Collection<String> values) {
        this.headerName = headerName;
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
    }

    @Nonnull
    @Override
    public String getName() {
        return this.headerName;
    }

    @Nonnull
    @Override
    public Collection<String> getValues() {
        return this.values;
    }

    @Nonnull
    @Override
    public Header clear() {
        return new DefaultHeader(this.headerName, Collections.emptyList());
    }

    @Override
    public String get() {
        if (this.values.isEmpty()) {
            return null;
        }
        return this.values.get(0);
    }

    @Override
    public int compareTo(@Nullable Header other) {
        if (other == null) {
            return 1;
        }
        return this.headerName.compareToIgnoreCase(other.getName());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Header) {
            final Header that = (Header) obj;
            if (!this.headerName.equalsIgnoreCase(that.getName())) {
                return false;
            }
            return this.values.equals(that.getValues());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.headerName, this.values);
    }

    @Override
    public String toString() {
        return this.headerName + ":" + String.join(";", this.values);
    }
}
