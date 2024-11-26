package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.time.Duration;

/**
 * @author linjpxc
 */
public interface RequestConfig extends Serializable {

    boolean isRedirectsEnabled();

    Duration getRequestTimeout();

    Duration getConnectTimeout();

    Duration getResponseTimeout();

    @Nonnull
    static Builder builder() {
        return new RequestConfigBuilder();
    }

    interface Builder {

        @Nonnull
        Builder redirectsEnabled(boolean enabled);

        @Nonnull
        Builder requestTimeout(Duration timeout);

        @Nonnull
        Builder connectTimeout(Duration timeout);

        @Nonnull
        Builder responseTimeout(Duration timeout);

        @Nonnull
        RequestConfig build();
    }
}
