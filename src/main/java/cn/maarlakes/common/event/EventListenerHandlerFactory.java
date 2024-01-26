package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author linjpxc
 */
public interface EventListenerHandlerFactory {

    @Nonnull
    List<EventListenerHandler> getListenerHandlers();
}
