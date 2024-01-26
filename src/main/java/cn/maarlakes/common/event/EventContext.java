package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * @author linjpxc
 */
public interface EventContext extends Serializable {

    <K, V> void setAttribute(@Nonnull K key, V value);

    <K, V> V getAttribute(@Nonnull K key);
}
