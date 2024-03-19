package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.net.URLEncoder;

/**
 * @author linjpxc
 */
public final class RequestParams {
    private RequestParams() {
    }

    @Nonnull
    public static String format(@Nonnull Iterable<? extends NameValuePair> params) {
        return format(params, "utf-8");
    }

    @Nonnull
    public static String format(@Nonnull Iterable<? extends NameValuePair> params, @Nonnull String charset) {
        final StringBuilder builder = new StringBuilder();
        try {
            for (NameValuePair param : params) {
                builder.append(URLEncoder.encode(param.getName(), charset)).append("=").append(URLEncoder.encode(param.getValue(), charset)).append("&");
            }
            if (builder.length() > 0) {
                return builder.substring(0, builder.length() - 1);
            }
            return "";
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
