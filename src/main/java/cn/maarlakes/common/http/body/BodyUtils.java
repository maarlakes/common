package cn.maarlakes.common.http.body;

import cn.maarlakes.common.http.NameValuePair;
import jakarta.annotation.Nonnull;

import java.net.URLEncoder;

/**
 * @author linjpxc
 */
public final class BodyUtils {
    private BodyUtils() {
    }

    @Nonnull
    public static String formatParams(@Nonnull Iterable<? extends NameValuePair> params) {
        final StringBuilder builder = new StringBuilder();
        for (NameValuePair param : params) {
            builder.append(param.getName()).append("=").append(param.getValue()).append("&");
        }
        if (builder.length() > 0) {
            return builder.substring(0, builder.length() - 1);
        }
        return "";
    }

    @Nonnull
    public static String formatParamsEncode(@Nonnull Iterable<? extends NameValuePair> params) {
        return formatParamsEncode(params, "utf-8");
    }

    @Nonnull
    public static String formatParamsEncode(@Nonnull Iterable<? extends NameValuePair> params, @Nonnull String charset) {
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

    @Nonnull
    public static byte[] contentAsBytes(@Nonnull CharSequence content) {
        return contentAsBytes(content, "utf-8");
    }

    @Nonnull
    public static byte[] contentAsBytes(@Nonnull CharSequence content, @Nonnull String charset) {
        try {
            return content.toString().getBytes(charset);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
