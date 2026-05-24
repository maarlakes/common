package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@link HttpHeaders} 的默认实现，使用大小写不敏感的 {@link TreeMap} 存储头映射。
 *
 * <p>在构造时即对所有传入的头按名称（忽略大小写）排序并去重，
 * 保证通过 {@link #getHeader(String)} 查找时不依赖名称的大小写。
 * 支持从多值映射（如 {@code Map<String, Collection<String>>}）直接构建。</p>
 *
 * @author linjpxc
 */
public class DefaultHttpHeaders implements HttpHeaders {
    private static final long serialVersionUID = -1545168488689288645L;

    private final Map<String, ? extends Header> headers;

    public DefaultHttpHeaders(@Nonnull Map<String, Header> headers) {
        final Map<String, Header> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        map.putAll(headers);
        this.headers = map;
    }

    /**
     * 从多值映射构建 HttpHeaders，每个键对应一组头值。
     *
     * @param map 头名称到值集合的映射
     * @return 构建好的 HttpHeaders 实例
     */
    public static HttpHeaders fromMultiMap(@Nonnull Map<String, ? extends Collection<String>> map) {
        final Map<String, Header> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, ? extends Collection<String>> entry : map.entrySet()) {
            result.put(entry.getKey(), new DefaultHeader(entry.getKey(), entry.getValue()));
        }
        return new DefaultHttpHeaders(result);
    }

    @Override
    public boolean isEmpty() {
        return this.headers.isEmpty();
    }

    @Override
    public Header getHeader(@Nonnull String name) {
        final Header header = this.headers.get(name);
        return header != null ? header : new DefaultHeader(name, Collections.emptyList());
    }

    @Override
    public String toString() {
        return this.headers.values()
                .stream().map(Object::toString)
                .collect(Collectors.joining("\r\n"));
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public Iterator<Header> iterator() {
        return (Iterator<Header>) this.headers.values().iterator();
    }
}
