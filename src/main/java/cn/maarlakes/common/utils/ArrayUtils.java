package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.lang.reflect.Array;

/**
 * @author linjpxc
 */
public final class ArrayUtils {
    private ArrayUtils() {

    }

    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(@Nonnull Iterable<T> iterable) {
        Class<?> type = null;
        for (T item : iterable) {
            type = item.getClass();
            break;
        }
        if (type == null){
            throw new IllegalArgumentException("iterable is null or empty");
        }
        final Class<?> clazz = type;
        return CollectionUtils.stream(iterable).toArray(length -> (T[]) Array.newInstance(clazz, length));
    }
}
