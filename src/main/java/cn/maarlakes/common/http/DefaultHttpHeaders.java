package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.stream.Collectors;

/**
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
