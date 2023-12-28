package cn.maarlakes.common.factory.json;

import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.Lazy;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * @author linjpxc
 */
public final class JsonFactories {
    private JsonFactories() {
    }

    private static final Supplier<JsonProvider> PROVIDER = Lazy.of(() -> SpiServiceLoader.loadShared(JsonProvider.class).firstOptional().orElseThrow(() -> new IllegalStateException("No JsonProvider implementation found")));

    @Nonnull
    public static <T> String toJson(@Nonnull T value) {
        return PROVIDER.get().toJson(value);
    }

    @Nonnull
    public static Object toModel(@Nonnull CharSequence json) {
        return toModel(json, Object.class);
    }

    @Nonnull
    public static <T> T toModel(@Nonnull CharSequence json, @Nonnull Class<T> type) {
        return PROVIDER.get().toModel(json, type);
    }

    @Nonnull
    public static List<Object> toList(@Nonnull CharSequence json) {
        return toList(json, Object.class);
    }

    @Nonnull
    public static <T> List<T> toList(@Nonnull CharSequence json, @Nonnull Class<T> type) {
        return PROVIDER.get().toList(json, type);
    }

    @Nonnull
    public static Set<Object> toSet(@Nonnull CharSequence json) {
        return toSet(json, Object.class);
    }

    @Nonnull
    public static <T> Set<T> toSet(@Nonnull CharSequence json, @Nonnull Class<T> type) {
        return PROVIDER.get().toSet(json, type);
    }

    @Nonnull
    public static Object[] toArray(@Nonnull CharSequence json) {
        return toArray(json, Object.class);
    }

    @Nonnull
    public static <T> T[] toArray(@Nonnull CharSequence json, @Nonnull Class<T> type) {
        return PROVIDER.get().toArray(json, type);
    }

    @Nonnull
    public static Map<String, Object> toMap(@Nonnull CharSequence json) {
        return toMap(json, Object.class);
    }

    @Nonnull
    public static <V> Map<String, V> toMap(@Nonnull CharSequence json, @Nonnull Class<V> valueType) {
        return toMap(json, String.class, valueType);
    }

    @Nonnull
    public static <K, V> Map<K, V> toMap(@Nonnull CharSequence json, @Nonnull Class<K> keyType, @Nonnull Class<V> valueType) {
        return PROVIDER.get().toMap(json, keyType, valueType);
    }
}
