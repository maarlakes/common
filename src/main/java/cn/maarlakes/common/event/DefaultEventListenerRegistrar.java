package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 默认的事件监听器注册器。
 *
 * <p><b>EventPublisher 创建策略</b>
 * <p>本类的默认策略是：每次调用 {@link #publisher()} 时都会创建一个新的 {@link EventPublisher} 实例。
 * 这意味着事件类型到监听器列表的缓存（{@link DefaultEventPublisher#eventInvokerCache}）
 * 生命周期仅限于该次返回的发布者对象，不会跨调用复用。
 * 若需长期复用缓存，调用方应自行持有返回的 {@link EventPublisher} 实例，而非反复调用此方法。
 *
 * <p><b>异常处理策略</b>
 * <p>本模块遵循"发布者只管发布，订阅者自行负责异常"的设计原则。
 * {@link DefaultEventPublisher} 默认仅将异常记录到日志，不会阻断其他监听器的执行，
 * 也不向事件发布方抛出受检异常或强制要求处理。异步派发时异常会被静默完成
 *（{@code CompletableFuture} 通过 {@code exceptionally} 吞掉），
 * 同步派发时异常虽然会抛出，但原则上应由各订阅方法自行捕获并处理业务异常。
 *
 * @author linjpxc
 */
public class DefaultEventListenerRegistrar implements EventListenerRegistrar {

    private static final Logger log = LoggerFactory.getLogger(DefaultEventListenerRegistrar.class);

    private final EventDispatcher dispatcher;
    private final EventListenerHandlerFactory listenerHandlersFactory;
    private final EventPublisherFactory eventPublisherFactory;
    private final ConcurrentMap<Object, List<EventInvoker>> invokers = new ConcurrentHashMap<>();

    public DefaultEventListenerRegistrar(@Nonnull EventDispatcher dispatcher, @Nonnull EventListenerHandlerFactory listenerHandlersFactory, @Nonnull EventPublisherFactory eventPublisherFactory) {
        this.dispatcher = dispatcher;
        this.listenerHandlersFactory = listenerHandlersFactory;
        this.eventPublisherFactory = eventPublisherFactory;
    }

    @Override
    public <L> void register(@Nonnull L listener) {
        final List<EventInvoker> listenerInvokers = getListenerInvoker(listener);
        this.invokers.put(listener, listenerInvokers);
        log.info("Registered event listener: {} with {} invoker(s)", listener.getClass().getName(), listenerInvokers.size());
    }

    @Override
    public <L> void unregister(@Nonnull L listener) {
        final List<EventInvoker> removed = this.invokers.remove(Objects.requireNonNull(listener));
        if (removed != null) {
            log.info("Unregistered event listener: {} ({} invoker(s))", listener.getClass().getName(), removed.size());
        }
    }

    @Override
    public void unregisterAll() {
        final int count = this.invokers.size();
        this.invokers.clear();
        log.info("Unregistered all event listeners ({} listener(s))", count);
    }

    @Nonnull
    @Override
    public EventPublisher publisher() {
        return this.eventPublisherFactory.createPublisher(this.dispatcher, this.invokers.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
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
}
