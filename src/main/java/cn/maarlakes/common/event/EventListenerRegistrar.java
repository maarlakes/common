package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface EventListenerRegistrar {

    <L> void register(@Nonnull L listener);

    <L> void unregister(@Nonnull L listener);

    void unregisterAll();
}
