package cn.maarlakes.common.event;

import cn.maarlakes.common.AnnotationOrderComparator;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public class DefaultEventListenerRegistrar implements EventListenerRegistrar, EventPublisherFactory {

    private final EventPublisher eventPublisher;
    private final EventListenerHandlerFactory listenerHandlersFactory;
    private final ConcurrentMap<Object, List<EventInvoker>> invokers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class<?>, Collection<EventInvoker>> eventInvokerCache = new ConcurrentHashMap<>();

    public DefaultEventListenerRegistrar(@Nonnull EventDispatcher dispatcher, EventListenerHandlerFactory listenerHandlersFactory) {
        this.eventPublisher = new DefaultEventPublisher(dispatcher);
        this.listenerHandlersFactory = listenerHandlersFactory;
    }

    @Override
    public <L> void register(@Nonnull L listener) {
        this.invokers.compute(listener, (k, listenerInvokers) -> getListenerInvoker(listener));
        this.eventInvokerCache.clear();
    }

    @Override
    public <L> void unregister(@Nonnull L listener) {
        this.invokers.remove(Objects.requireNonNull(listener));
        this.eventInvokerCache.clear();
    }

    @Override
    public void unregisterAll() {
        this.invokers.clear();
        this.eventInvokerCache.clear();
    }

    @Nonnull
    @Override
    public EventPublisher getPublisher() {
        return this.eventPublisher;
    }

    private <L> List<EventInvoker> getListenerInvoker(L listener) {
        final List<EventListenerHandler> listenerHandlers = this.listenerHandlersFactory.getListenerHandlers();
        if (listenerHandlers.isEmpty()) {
            throw new IllegalArgumentException("No listener handlers");
        }
        final List<EventInvoker> invokers = new ArrayList<>();
        for (final EventListenerHandler handler : listenerHandlers) {
            invokers.addAll(handler.getInvokers(listener));
        }
        if (invokers.isEmpty()) {
            throw new IllegalArgumentException("No listener invokers were found for listener <" + listener + ">");
        }
        return Collections.unmodifiableList(invokers);
    }

    private class DefaultEventPublisher implements EventPublisher {
        private final EventDispatcher dispatcher;

        private DefaultEventPublisher(@Nonnull EventDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public <E> void publish(@Nonnull E event) {
            eventInvokerCache.computeIfAbsent(event.getClass(), eventType -> invokers.values().stream()
                            .flatMap(Collection::stream)
                            .filter(item -> item.supportedEvent(eventType))
                            .sorted(AnnotationOrderComparator.getInstance())
                            .collect(Collectors.toList()))
                    .forEach(invoker -> {
                        try {
                            this.dispatcher.dispatch(invoker, event);
                        } catch (Throwable error) {
                            if (error instanceof EventException) {
                                throw (EventException) error;
                            }
                            throw new EventException(error);
                        }
                    });
        }
    }
}
