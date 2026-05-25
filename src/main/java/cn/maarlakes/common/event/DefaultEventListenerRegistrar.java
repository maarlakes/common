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

    /** 事件分发器 */
    private final EventDispatcher dispatcher;
    /** 监听器处理器工厂，用于获取所有可用的 {@link EventListenerHandler} */
    private final EventListenerHandlerFactory listenerHandlersFactory;
    /** 事件发布器工厂，用于创建 {@link EventPublisher} */
    private final EventPublisherFactory eventPublisherFactory;
    /** 已注册的监听器到其调用器列表的映射 */
    private final ConcurrentMap<Object, List<EventInvoker>> invokers = new ConcurrentHashMap<>();

    /**
     * 创建事件监听器注册器。
     *
     * @param dispatcher            事件分发器
     * @param listenerHandlersFactory 监听器处理器工厂
     * @param eventPublisherFactory 事件发布器工厂
     */
    public DefaultEventListenerRegistrar(@Nonnull EventDispatcher dispatcher, @Nonnull EventListenerHandlerFactory listenerHandlersFactory, @Nonnull EventPublisherFactory eventPublisherFactory) {
        this.dispatcher = dispatcher;
        this.listenerHandlersFactory = listenerHandlersFactory;
        this.eventPublisherFactory = eventPublisherFactory;
    }

    @Override
    public <L> void register(@Nonnull L listener) {
        final List<EventInvoker> listenerInvokers = getListenerInvoker(listener);
        this.invokers.put(listener, listenerInvokers);
        log.info("已注册事件监听器: {}, 包含 {} 个调用器", listener.getClass().getName(), listenerInvokers.size());
    }

    @Override
    public <L> void unregister(@Nonnull L listener) {
        final List<EventInvoker> removed = this.invokers.remove(Objects.requireNonNull(listener));
        if (removed != null) {
            log.info("已注销事件监听器: {} ({} 个调用器)", listener.getClass().getName(), removed.size());
        }
    }

    @Override
    public void unregisterAll() {
        final int count = this.invokers.size();
        this.invokers.clear();
        log.info("已注销所有事件监听器 ({} 个监听器)", count);
    }

    /**
     * {@inheritDoc}
     *
     * <p>每次调用都会创建新的 {@link EventPublisher} 实例，内部包含当前所有已注册监听器的调用器快照。
     * 若需复用发布器，调用方应自行持有返回的实例。
     */
    @Nonnull
    @Override
    public EventPublisher publisher() {
        return this.eventPublisherFactory.createPublisher(this.dispatcher, this.invokers.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * 从监听器对象中提取所有事件调用器。
     *
     * <p>通过 {@link EventListenerHandlerFactory} 获取所有处理器，逐个提取调用器。
     * 如果没有可用的处理器或监听器中未发现任何监听方法，将抛出 {@link IllegalArgumentException}。
     *
     * @param listener  监听器对象
     * @param <L>       监听器类型
     * @return 不可变的调用器列表
     * @throws IllegalArgumentException 没有可用的处理器或监听器中无监听方法
     */
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
