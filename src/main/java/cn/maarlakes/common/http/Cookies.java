package cn.maarlakes.common.http;

import cn.maarlakes.common.factory.datetime.DateTimeFactories;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
 * Cookie 解析工具类，从 HTTP 响应头中解析 Cookie 实例。
 *
 * <p>支持解析 Set-Cookie 和 Set-Cookie2 头格式，自动识别并处理
 * domain、path、max-age、secure、httponly、samesite、version、expires 等 Cookie 属性。
 * 内部使用预注册的属性处理器映射表来分派各属性的解析逻辑。</p>
 *
 * @author linjpxc
 */
public final class Cookies {
    private Cookies() {
    }

    private static final Map<String, BiConsumer<Cookie.Builder, String>> COOKIE_VALUE_HANDLER = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    static {
        COOKIE_VALUE_HANDLER.put("domain", Cookie.Builder::domain);
        COOKIE_VALUE_HANDLER.put("path", Cookie.Builder::path);
        COOKIE_VALUE_HANDLER.put("max-age", (builder, value) -> {
            try {
                if (value != null && !value.isEmpty()) {
                    builder.maxAge(Long.parseLong(value));
                }
            } catch (NumberFormatException ignored) {
            }
        });
        COOKIE_VALUE_HANDLER.put("secure", (builder, value) -> builder.isSecure(true));
        COOKIE_VALUE_HANDLER.put("httponly", (builder, value) -> builder.isHttpOnly(true));
        COOKIE_VALUE_HANDLER.put("samesite", (builder, value) -> builder.sameSite(Cookie.SameSite.of(value)));
        COOKIE_VALUE_HANDLER.put("same-site", (builder, value) -> builder.sameSite(Cookie.SameSite.of(value)));
        COOKIE_VALUE_HANDLER.put("version", (builder, value) -> {
            try {
                if (value != null && !value.isEmpty()) {
                    builder.version(Integer.parseInt(value));
                }
            } catch (NumberFormatException ignored) {
            }
        });
        COOKIE_VALUE_HANDLER.put("expires", (builder, value) -> {
            if (value != null && !value.isEmpty()) {
                try {
                    builder.expires(DateTimeFactories.parse(value));
                } catch (Exception ignored) {
                }
            }
        });
    }

    /**
     * 解析单个 Set-Cookie 头值字符串为 {@link Cookie} 实例。
     *
     * @param cookieValue Set-Cookie 头的值
     * @return 解析后的 Cookie 实例，输入为空时返回 {@code null}
     * @throws IllegalArgumentException Cookie 格式不合法时抛出
     */
    public static Cookie parse(@Nonnull String cookieValue) {
        cookieValue = cookieValue.trim();
        if (cookieValue.isEmpty()) {
            return null;
        }
        final StringTokenizer tokenizer = new StringTokenizer(cookieValue, ";");
        String nameValuePair;
        Cookie.Builder builder;
        try {
            nameValuePair = tokenizer.nextToken();
            int index = nameValuePair.indexOf('=');
            if (index != -1) {
                String name = nameValuePair.substring(0, index).trim();
                String value = nameValuePair.substring(index + 1).trim();
                builder = Cookie.builder(name).value(value);
            } else {
                throw new IllegalArgumentException("Invalid cookie name-cookieValue pair");
            }
        } catch (NoSuchElementException ignored) {
            throw new IllegalArgumentException("Empty cookie header string");
        }

        while (tokenizer.hasMoreTokens()) {
            nameValuePair = tokenizer.nextToken();
            int index = nameValuePair.indexOf('=');
            final String name;
            final String value;
            if (index != -1) {
                name = nameValuePair.substring(0, index).trim();
                value = nameValuePair.substring(index + 1).trim();
            } else {
                name = nameValuePair.trim();
                value = null;
            }
            final BiConsumer<Cookie.Builder, String> consumer = COOKIE_VALUE_HANDLER.get(name);
            if (consumer != null) {
                consumer.accept(builder, value);
            }
        }
        return builder.build();
    }

    /**
     * 从 HTTP 响应头集合中解析所有 Cookie，合并 Set-Cookie 和 Set-Cookie2 两个头的值。
     *
     * @param headers HTTP 响应头集合
     * @return 解析出的 Cookie 列表
     */
    public static List<Cookie> parseFromHeaders(@Nonnull HttpHeaders headers) {
        final List<Cookie> cookies = new ArrayList<>();
        final Header setCookie = headers.getHeader("Set-Cookie");
        if (setCookie != null) {
            for (String value : setCookie.getValues()) {
                final Cookie cookie = parse(value);
                if (cookie != null) {
                    cookies.add(cookie);
                }
            }
        }
        final Header setCookie2 = headers.getHeader("set-cookie2");
        if (setCookie2 != null) {
            for (String value : setCookie2.getValues()) {
                final Cookie cookie = parse(value);
                if (cookie != null) {
                    cookies.add(cookie);
                }
            }
        }
        return cookies;
    }
}
