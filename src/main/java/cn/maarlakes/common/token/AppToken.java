package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * @author linjpxc
 */
public interface AppToken<A, T> extends Serializable {

    @Nonnull
    A getAppId();

    @Nonnull
    T getToken();
}
