package cn.maarlakes.common.factory.json;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author linjpxc
 */
public class FastjsonProvider implements JsonProvider {
    @Nonnull
    @Override
    public <T> String toJson(@Nonnull T value) {

        return JSON.toJSONString(value);
    }

    @Nonnull
    @Override
    public <T> T toModel(@Nonnull CharSequence json, @Nonnull Class<T> type) {
        return JSON.parseObject(json.toString(), type);
    }

    @Nonnull
    @Override
    public <T> List<T> toList(@Nonnull CharSequence json, @Nonnull Class<T> type) {
        return JSON.parseArray(json.toString(), type);
    }

    @Nonnull
    @Override
    public <T> Set<T> toSet(@Nonnull CharSequence json, @Nonnull Class<T> type) {
        return JSON.parseObject(json.toString(), new TypeReference<Set<T>>() {
        });
    }

    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull CharSequence json, @Nonnull Class<T> type) {
        return JSON.parseArray(json.toString()).toArray(type);
    }

    @Nonnull
    @Override
    public <K, V> Map<K, V> toMap(@Nonnull CharSequence json, @Nonnull Class<K> keyType, @Nonnull Class<V> valueType) {
        return JSON.parseObject(json.toString(), new TypeReference<Map<K, V>>() {
        });
    }
}
