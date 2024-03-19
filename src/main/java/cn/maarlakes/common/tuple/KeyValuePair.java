package cn.maarlakes.common.tuple;

import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author linjpxc
 */
public class KeyValuePair<K, V> implements Serializable {
    private static final long serialVersionUID = -52505007629865251L;

    @Nonnull
    private final K key;
    private final V value;

    public KeyValuePair(@Nonnull K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Nonnull
    public K key() {
        return this.key;
    }

    public V value() {
        return this.value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof KeyValuePair) {
            final KeyValuePair<?, ?> that = (KeyValuePair<?, ?>) object;
            return Objects.equals(key, that.key) && Objects.equals(value, that.value);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return this.key + "=" + this.value;
    }
}
