package cn.maarlakes.common.utils;


import java.lang.reflect.Array;

/**
 * @author linjpxc
 */
public final class ArrayUtils {
    private ArrayUtils() {

    }

    @SuppressWarnings("unchecked")
    public static <T> T[] toArray(Iterable<T> iterable) {
        if (iterable == null) {
            throw new IllegalArgumentException("iterable is null");
        }
        Class<?> type = null;
        for (T item : iterable) {
            if (item != null) {
                type = item.getClass();
                break;
            }
        }
        if (type == null) {
            throw new IllegalArgumentException("iterable is empty or all elements are null");
        }
        final Class<?> clazz = type;
        return CollectionUtils.stream(iterable).toArray(length -> (T[]) Array.newInstance(clazz, length));
    }

    public static <T> void reverse(T[] array) {
        if (array == null) {
            throw new IllegalArgumentException("array is null");
        }
        final int count = array.length / 2;
        for (int i = 0; i < count; i++) {
            final T temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }
}
