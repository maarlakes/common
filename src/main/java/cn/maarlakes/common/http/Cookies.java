package cn.maarlakes.common.http;

import cn.maarlakes.common.factory.datetime.DateTimeFactories;
import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.function.BiConsumer;

/**
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
            if (value != null && !value.isEmpty()) {
                builder.maxAge(Long.parseLong(value));
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
}
