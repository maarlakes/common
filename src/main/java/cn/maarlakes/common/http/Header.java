package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * @author linjpxc
 */
public interface Header extends Comparable<Header>, Serializable {

    @Nonnull
    String getName();

    @Nonnull
    Collection<String> getValues();

    @Nonnull
    Header clear();

    String get();

    default String get(@Nonnull String defaultValue) {
        return this.get(() -> defaultValue);
    }

    default String get(@Nonnull Supplier<String> defaultValueFactory) {
        final String s = this.get();
        if (s == null) {
            return defaultValueFactory.get();
        }
        return s;
    }
}
