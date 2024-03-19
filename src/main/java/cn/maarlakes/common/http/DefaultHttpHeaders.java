package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
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

    @Override
    public boolean isEmpty() {
        return this.headers.isEmpty();
    }

    @Override
    public Header getHeader(@Nonnull String name) {
        return this.headers.get(name);
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
