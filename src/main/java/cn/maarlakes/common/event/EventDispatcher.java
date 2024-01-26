package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface EventDispatcher {

    <E> void dispatch(@Nonnull EventInvoker invoker, @Nonnull E event);
}
