package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author linjpxc
 */
public interface HttpHeaders extends Iterable<Header>, Serializable {

    boolean isEmpty();

    Header getHeader(@Nonnull String name);

    default boolean containsHeader(@Nonnull String name) {
        return this.getHeader(name) != null;
    }

    default int size() {
        int count = 0;
        for (Header ignored : this) {
            count++;
        }
        return count;
    }

    @Nonnull
    default Collection<? extends Header> getAllHeaders() {
        java.util.List<Header> list = new java.util.ArrayList<>();
        for (Header header : this) {
            list.add(header);
        }
        return list;
    }
}
