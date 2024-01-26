package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public class CombinedEventListenerHandlerFactory implements EventListenerHandlerFactory {

    private final List<EventListenerHandlerFactory> factories;

    public CombinedEventListenerHandlerFactory(@Nonnull List<EventListenerHandlerFactory> factories) {
        this.factories = factories;
    }

    @Nonnull
    @Override
    public List<EventListenerHandler> getListenerHandlers() {
        return this.factories.stream().flatMap(item -> item.getListenerHandlers().stream()).collect(Collectors.toList());
    }
}
