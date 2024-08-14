package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author linjpxc
 */
public final class CollectionUtils {

    private CollectionUtils() {
    }

    public static <T> boolean isEmpty(Collection<T> collection) {
        return collection == null || collection.isEmpty();
    }

    public static <T> boolean isNotEmpty(Collection<T> collection) {
        return !isEmpty(collection);
    }

    public static <K, V> boolean isEmpty(Map<K, V> map) {
        return map == null || map.isEmpty();
    }

    public static <K, V> boolean isNotEmpty(Map<K, V> map) {
        return !isEmpty(map);
    }

    public static <T> Stream<T> stream(@Nonnull Iterable<T> iterator) {
        return stream(iterator, false);
    }

    public static <T> Stream<T> stream(@Nonnull Iterable<T> iterator, boolean parallel) {
        if (iterator instanceof Collection) {
            if (parallel) {
                return ((Collection<T>) iterator).parallelStream();
            }
            return ((Collection<T>) iterator).stream();
        }
        return StreamSupport.stream(iterator.spliterator(), parallel);
    }
}
