package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * @author linjpxc
 */
public interface NameValuePair extends Serializable {

    @Nonnull
    String getName();

    String getValue();

    @Nonnull
    static NameValuePair of(String name, String value) {
        return new DefaultNameValuePair(name, value);
    }
}
