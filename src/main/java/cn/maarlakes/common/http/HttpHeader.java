package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author linjpxc
 */
public interface HttpHeader extends Comparable<HttpHeader>, Serializable {

    @Nonnull
    String getHeaderName();

    @Nonnull
    Collection<String> getValues();

    HttpHeader removeValue(@Nonnull String... value);

    HttpHeader addValue(@Nonnull String... value);
}
