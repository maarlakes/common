package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface EventInvoker {

    boolean supportedAsync();

    boolean supportedEvent(@Nonnull Class<?> eventType);

    <E> void invoke(@Nonnull E event);
}
