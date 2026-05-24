package cn.maarlakes.common.http;

import cn.maarlakes.common.utils.CollectionUtils;
import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * Content-Type 相关的工具方法，提供字符集提取和序列化功能。
 *
 * <p>将 {@link ContentType} 对象序列化为符合 HTTP 规范的字符串格式
 * （如 "text/html; charset=utf-8; param=value"），
 * 并提供带默认值的字符集提取方法。</p>
 *
 * @author linjpxc
 */
public final class ContentTypes {
    private ContentTypes() {
    }

    /**
     * 提取 ContentType 中的字符集，未指定时默认返回 "utf-8"。
     */
    @Nonnull
    public static String getCharset(@Nonnull ContentType contentType) {
        return getCharset(contentType, "utf-8");
    }

    /**
     * 提取 ContentType 中的字符集，未指定时返回传入的默认字符集。
     */
    @Nonnull
    public static String getCharset(@Nonnull ContentType contentType, @Nonnull String defaultCharset) {
        final String charset = contentType.getCharset();
        return charset == null || charset.isEmpty() ? defaultCharset : charset;
    }

    /**
     * 将 ContentType 序列化为标准格式字符串，包含媒体类型、字符集和附加参数。
     */
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
