package cn.maarlakes.common.http.body;

import cn.maarlakes.common.http.NameValuePair;
import jakarta.annotation.Nonnull;

import java.net.URLEncoder;

/**
 * 消息体处理的工具类，提供表单参数格式化和字符序列转字节数组等基础方法。
 *
 * <p>供 {@link DefaultTextBody}、{@link UrlEncodedFormEntityBody}、{@code DefaultTextPart} 等
 * body/multipart 类共享使用。所有方法均为静态方法，不可实例化。</p>
 *
 * @author linjpxc
 */
public final class BodyUtils {
    /** 工具类禁止实例化 */
    private BodyUtils() {
    }

    /**
     * 将键值对集合格式化为 {@code name=value&name2=value2} 形式的字符串，不进行编码。
     */
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

    /**
     * 对键值对集合进行 URL 编码后格式化，默认使用 UTF-8 字符集。
     */
    @Nonnull
    public static String formatParamsEncode(@Nonnull Iterable<? extends NameValuePair> params) {
        return formatParamsEncode(params, "utf-8");
    }

    /**
     * 对键值对集合进行 URL 编码后格式化，使用指定字符集。
     * 编码异常时抛出 {@link IllegalStateException}。
     */
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

    /**
     * 将字符序列转换为字节数组，默认使用 UTF-8 字符集。
     */
    @Nonnull
    public static byte[] contentAsBytes(@Nonnull CharSequence content) {
        return contentAsBytes(content, "utf-8");
    }

    /**
     * 将字符序列按指定字符集转换为字节数组。
     * 编码异常时抛出 {@link IllegalStateException}。
     */
    @Nonnull
    public static byte[] contentAsBytes(@Nonnull CharSequence content, @Nonnull String charset) {
        try {
            return content.toString().getBytes(charset);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
