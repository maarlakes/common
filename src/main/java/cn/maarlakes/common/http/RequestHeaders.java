package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public final class RequestHeaders {
    private RequestHeaders() {
    }

    @Nonnull
    public static String toString(@Nonnull Header header) {
        return String.join(";", header.getValues());
    }
}
