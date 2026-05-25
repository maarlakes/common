package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link EventContext} 的默认实现，基于 {@link ConcurrentHashMap} 提供线程安全的属性存取。
 *
 * <p>可直接继承此类，也可以让事件类直接实现 {@link EventContext} 接口。
 *
 * @author linjpxc
 */
public class DefaultEventContext implements EventContext {
    private static final long serialVersionUID = -1234593964894821133L;

    /** 属性存储，线程安全 */
    protected final Map<Object, Object> attributes = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     *
     * <p>当 {@code value} 为 {@code null} 时，移除该键对应的属性。
     */
    @Override
    public <K, V> void setAttribute(@Nonnull K key, V value) {
        if (value == null) {
            this.attributes.remove(key);
        } else {
            this.attributes.put(key, value);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K, V> V getAttribute(@Nonnull K key) {
        return (V) this.attributes.get(key);
    }
}
