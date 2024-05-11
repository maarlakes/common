package cn.maarlakes.common.http;

import cn.maarlakes.common.utils.CollectionUtils;
import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * @author linjpxc
 */
public final class ContentTypes {
    private ContentTypes() {
    }

    @Nonnull
    public static String getCharset(@Nonnull ContentType contentType) {
        return getCharset(contentType, "utf-8");
    }

    @Nonnull
    public static String getCharset(@Nonnull ContentType contentType, @Nonnull String defaultCharset) {
        final String charset = contentType.getCharset();
        return charset == null || charset.isEmpty() ? defaultCharset : charset;
    }

    public static String toString(@Nonnull ContentType contentType) {
        final StringBuilder builder = new StringBuilder(contentType.getMediaType());
        appendParameter(builder, contentType);
        return builder.toString();
    }

    private static void appendParameter(@Nonnull StringBuilder builder, @Nonnull ContentType contentType) {
        boolean hasCharset = false;
        if (contentType.getCharset() != null) {
            hasCharset = true;
            if (builder.length() > 0) {
                builder.append(";");
            }
            builder.append("charset=").append(contentType.getCharset());
        }
        final Collection<NameValuePair> parameters = contentType.getParameters();
        if (CollectionUtils.isNotEmpty(parameters)) {
            for (NameValuePair parameter : parameters) {
                if (hasCharset && "charset".equalsIgnoreCase(parameter.getName())) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(";");
                }
                builder.append(parameter.getName()).append("=").append(parameter.getValue());
            }
        }
    }
}
