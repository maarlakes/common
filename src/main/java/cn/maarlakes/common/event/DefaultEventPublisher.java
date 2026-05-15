package cn.maarlakes.common.event;

import cn.maarlakes.common.AnnotationOrderComparator;
import cn.maarlakes.common.utils.CollectionUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 默认事件发布者实现。
 *
 * <p><b>阻塞语义</b>
 * <p>{@link #publish(Object)} 方法在派发事件时，会等待所有标记了 {@code @EventDispatch(async = true)} 的
 * 异步监听器执行完毕后才会返回。因此调用方不应假设 publish 是 fire-and-forget 的无阻塞调用。
 *
 * @author linjpxc
 */
public class DefaultEventPublisher implements EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(DefaultEventPublisher.class);

    private final EventDispatcher dispatcher;
    private final List<? extends EventInvoker> invokers;

    private final ConcurrentMap<Class<?>, Collection<EventInvoker>> eventInvokerCache = new ConcurrentHashMap<>();

    public DefaultEventPublisher(@Nonnull EventDispatcher dispatcher, @Nonnull List<? extends EventInvoker> invokers) {
        this.dispatcher = Objects.requireNonNull(dispatcher);
        this.invokers = Objects.requireNonNull(invokers);
    }

    @Override
    public <E> void publish(@Nonnull E event) {
        final Class<?> eventType = event.getClass();
        if (log.isDebugEnabled()) {
            log.debug("Publishing event: {}", eventType.getName());
        }
        if (log.isTraceEnabled()) {
            log.trace("Event payload: {}", event);
        }

        final Collection<EventInvoker> eventInvokers = eventInvokerCache.computeIfAbsent(eventType, type -> invokers.stream()
                .filter(item -> item.supportedEvent(type))
                .sorted(AnnotationOrderComparator.getInstance())
                .collect(Collectors.toList()));

        if (log.isDebugEnabled()) {
            log.debug("Matched {} listener(s) for event: {}", eventInvokers.size(), eventType.getName());
        }

        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (EventInvoker invoker : eventInvokers) {
            final EventDispatch annotation = invoker.getAnnotation(EventDispatch.class);
            final boolean async = annotation != null && annotation.async();
            if (log.isDebugEnabled()) {
                log.debug("Dispatching event {} to listener {} [async={}]", eventType.getName(), invoker, async);
            }
            if (async) {
                futures.add(this.dispatcher.dispatchAsync(invoker, event).exceptionally(error -> {
                    this.handleException(event, invoker, error, true);
                    return null;
                }));
            } else {
                try {
                    this.dispatcher.dispatch(invoker, event);
                } catch (Exception e) {
                    this.handleException(event, invoker, e, false);
                }
            }
        }
        if (CollectionUtils.isNotEmpty(futures)) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        if (log.isDebugEnabled()) {
            log.debug("Event {} dispatched to all listeners", eventType.getName());
        }
    }

    protected <E> void handleException(@Nonnull E event, @Nonnull EventInvoker invoker, @Nonnull Throwable error, boolean async) {
        log.error("Failed to dispatch event {} to listener {} [async={}]", event.getClass().getName(), invoker, async, error);
    }
}
