package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Collection;

/**
 * HTTP 头部集合，支持按名称查询和迭代所有头部。
 *
 * <p>实现 {@link Iterable}，可直接在 for-each 循环中遍历所有头部。
 * 同名头部通过 {@link Header#getValues()} 返回的列表支持多值语义
 * （如 {@code Set-Cookie} 可以有多个值）。
 *
 * <p>头部名称比较通常不区分大小写，具体行为由实现决定。
 *
 * @author linjpxc
 */
public interface HttpHeaders extends Iterable<Header>, Serializable {

    /** 头部集合是否为空。 */
    boolean isEmpty();

    /**
     * 根据名称获取头部。不区分大小写。
     *
     * @param name 头部名称
     * @return 对应的头部，不存在时返回 null
     */
    Header getHeader(@Nonnull String name);

    /** 是否包含指定名称的头部。 */
    default boolean containsHeader(@Nonnull String name) {
        return this.getHeader(name) != null;
    }

    /** 头部数量。 */
    default int size() {
        int count = 0;
        for (Header ignored : this) {
            count++;
        }
        return count;
    }

    /** 获取所有头部的集合。 */
    @Nonnull
    default Collection<? extends Header> getAllHeaders() {
        java.util.List<Header> list = new java.util.ArrayList<>();
        for (Header header : this) {
            list.add(header);
        }
        return list;
    }
}
