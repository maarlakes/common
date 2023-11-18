package cn.maarlakes.common.factory.json;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author linjpxc
 */
public interface JsonProvider {

    @Nonnull
    <T> String toJson(@Nonnull T value);

    @Nonnull
    <T> T toModel(@Nonnull CharSequence json, @Nonnull Class<T> type);

    @Nonnull
    <T> List<T> toList(@Nonnull CharSequence json, @Nonnull Class<T> type);

    @Nonnull
    <T> Set<T> toSet(@Nonnull CharSequence json, @Nonnull Class<T> type);

    @Nonnull
    <T> T[] toArray(@Nonnull CharSequence json, @Nonnull Class<T> type);

    @Nonnull
    <K, V> Map<K, V> toMap(@Nonnull CharSequence json, @Nonnull Class<K> keyType, @Nonnull Class<V> valueType);
}
