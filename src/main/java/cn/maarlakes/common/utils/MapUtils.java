package cn.maarlakes.common.utils;

import cn.maarlakes.common.tuple.KeyValuePair;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author linjpxc
 */
public final class MapUtils {
    private MapUtils() {
        throw new IllegalStateException("No cn.maarlakes.common.utils.MapUtils instances for you!");
    }

    @SafeVarargs
    public static <K, V> Map<K, V> create(KeyValuePair<K, V>... pairs) {
        return create(HashMap::new, pairs);
    }

    @SafeVarargs
    public static <T extends Map<K, V>, K, V> T create(Supplier<T> creator, KeyValuePair<K, V>... pairs) {
        final T map = creator.get();
        for (KeyValuePair<K, V> pair : pairs) {
            map.put(pair.key(), pair.value());
        }
        return map;
    }

    public static <K, V> Map<K, V> create(Iterable<KeyValuePair<K, V>> pairs) {
        return create(HashMap::new, pairs);
    }

    public static <T extends Map<K, V>, K, V> T create(Supplier<T> creator, Iterable<KeyValuePair<K, V>> pairs) {
        final T map = creator.get();
        for (KeyValuePair<K, V> pair : pairs) {
            map.put(pair.key(), pair.value());
        }
        return map;
    }
}
