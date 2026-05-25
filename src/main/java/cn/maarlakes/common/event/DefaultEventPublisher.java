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
 * <p><b>异常处理策略</b>
 * <p>遵循"发布者只管发布，订阅者自行负责异常"的设计原则。
 * 单个监听器的异常不会阻断后续监听器的执行。异步分发时异常通过
 * {@code CompletableFuture.exceptionally} 静默处理；同步分发时捕获异常后调用
 * {@link #handleException(Object, EventInvoker, Throwable, boolean)} 记录日志。
 *
 * @author linjpxc
 */
public class DefaultEventPublisher implements EventPublisher {
    private static final Logger log = LoggerFactory.getLogger(DefaultEventPublisher.class);

    /** 事件分发器 */
    private final EventDispatcher dispatcher;

    /** 所有已注册的事件调用器 */
    private final List<? extends EventInvoker> invokers;

    /** 事件类型到匹配调用器的缓存，避免每次发布都重新匹配和排序 */
    private final ConcurrentMap<Class<?>, Collection<EventInvoker>> eventInvokerCache = new ConcurrentHashMap<>();

    /**
     * 创建事件发布器。
     *
     * @param dispatcher 事件分发器，不能为 null
     * @param invokers   所有已注册的事件调用器列表，不能为 null
     */
    public DefaultEventPublisher(@Nonnull EventDispatcher dispatcher, @Nonnull List<? extends EventInvoker> invokers) {
        this.dispatcher = Objects.requireNonNull(dispatcher);
        this.invokers = Objects.requireNonNull(invokers);
    }

    @Override
    public <E> void publish(@Nonnull E event) {
        final Class<?> eventType = event.getClass();
        if (log.isDebugEnabled()) {
            log.debug("开始发布事件: {}", eventType.getName());
        }
        if (log.isTraceEnabled()) {
            log.trace("事件载荷: {}", event);
        }

        // 按事件类型查找匹配的调用器（带缓存），并按 @Order 排序
        final Collection<EventInvoker> eventInvokers = eventInvokerCache.computeIfAbsent(eventType, type -> invokers.stream()
                .filter(item -> item.supportedEvent(type))
                .sorted(AnnotationOrderComparator.getInstance())
                .collect(Collectors.toList()));

        if (log.isDebugEnabled()) {
            log.debug("事件 {} 匹配到 {} 个监听器", eventType.getName(), eventInvokers.size());
        }

        // 逐个分发到匹配的监听器
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (EventInvoker invoker : eventInvokers) {
            final EventDispatch annotation = invoker.getAnnotation(EventDispatch.class);
            final boolean async = annotation != null && annotation.async();
            if (log.isDebugEnabled()) {
                log.debug("分发事件 {} 到监听器 {} [异步={}]", eventType.getName(), invoker, async);
            }
            if (async) {
                // 异步分发：提交到线程池，异常通过 exceptionally 处理
                futures.add(this.dispatcher.dispatchAsync(invoker, event).exceptionally(error -> {
                    this.handleException(event, invoker, error, true);
                    return null;
                }));
            } else {
                // 同步分发：在当前线程中执行，异常捕获后不阻断后续监听器
                try {
                    this.dispatcher.dispatch(invoker, event);
                } catch (Exception e) {
                    this.handleException(event, invoker, e, false);
                }
            }
        }

        // 等待所有异步监听器执行完毕
        if (CollectionUtils.isNotEmpty(futures)) {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        if (log.isDebugEnabled()) {
            log.debug("事件 {} 已分发到所有监听器", eventType.getName());
        }
    }

    /**
     * 处理事件分发过程中发生的异常。
     *
     * <p>默认行为是记录 ERROR 日志。子类可重写此方法以自定义异常处理策略。
     *
     * @param <E>     事件类型
     * @param event   事件对象
     * @param invoker 发生异常的调用器
     * @param error   异常对象
     * @param async   是否为异步分发时发生的异常
     */
    protected <E> void handleException(@Nonnull E event, @Nonnull EventInvoker invoker, @Nonnull Throwable error, boolean async) {
        log.error("事件分发失败: 事件={}, 监听器={}, 异步={}", event.getClass().getName(), invoker, async, error);
    }
}
