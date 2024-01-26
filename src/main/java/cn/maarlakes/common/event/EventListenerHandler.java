package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author linjpxc
 */
public interface EventListenerHandler {

    <L> List<EventInvoker> getInvokers(@Nonnull L listener);
}
