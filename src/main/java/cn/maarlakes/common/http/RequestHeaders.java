package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

/**
 * 请求头的格式化工具类，将 {@link Header} 的多值合并为逗号分隔的字符串。
 *
 * <p>用于将内部的多值头表示转换为符合 HTTP 规范的单行头值格式。</p>
 *
 * @author linjpxc
 */
public final class RequestHeaders {
    private RequestHeaders() {
    }

    /**
     * 将头对象的所有值以逗号加空格连接为单个字符串。
     */
    @Nonnull
    public static String toString(@Nonnull Header header) {
        return String.join(", ", header.getValues());
    }
}
