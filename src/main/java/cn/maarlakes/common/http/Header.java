package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * HTTP 头的统一抽象，表示一个 HTTP 头字段及其关联的值列表。
 *
 * <p>HTTP 头可能包含多个值（如 Accept-Encoding: gzip, deflate），
 * 因此通过 {@link #getValues()} 返回值集合。{@link #get()} 提供快捷访问首个值的便利方法。
 * 实现了 {@link Comparable} 以支持按头名称大小写不敏感排序。</p>
 *
 * @author linjpxc
 */
public interface Header extends Comparable<Header>, Serializable {

    /**
     * 返回头字段名称（如 "Content-Type"）。
     */
    @Nonnull
    String getName();

    /**
     * 返回头字段的所有值。
     */
    @Nonnull
    Collection<String> getValues();

    /**
     * 返回一个值列表为空的同名头副本，用于清除头值。
     */
    @Nonnull
    Header clear();

    /**
     * 返回头字段的第一个值，无值时返回 {@code null}。
     */
    String get();

    /**
     * 返回头字段的第一个值，无值时返回指定的默认值。
     */
    default String get(@Nonnull String defaultValue) {
        return this.get(() -> defaultValue);
    }

    /**
     * 返回头字段的第一个值，无值时通过工厂函数生成默认值。
     */
    default String get(@Nonnull Supplier<String> defaultValueFactory) {
        final String s = this.get();
        if (s == null) {
            return defaultValueFactory.get();
        }
        return s;
    }
}
