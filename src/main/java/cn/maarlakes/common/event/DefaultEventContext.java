package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;

/**
 * @author linjpxc
 */
public class DefaultEventContext implements EventContext {
    private static final long serialVersionUID = -1234593964894821133L;

    protected final Map<Object, Object> attributes = new HashMap<>();

    @Override
    public <K, V> void setAttribute(@Nonnull K key, V value) {
        this.attributes.put(key, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> V getAttribute(@Nonnull K key) {
        return (V) this.attributes.get(key);
    }
}
