package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

/**
 * 事件监听器注册中心，管理监听器的注册、注销和事件发布器的获取。
 *
 * <p>使用方通过此接口注册带有 {@link EventListener} 标注方法的监听器对象，
 * 然后通过 {@link #publisher()} 获取 {@link EventPublisher} 来发布事件。
 *
 * @author linjpxc
 * @see DefaultEventListenerRegistrar
 */
public interface EventListenerRegistrar {

    /**
     * 注册一个监听器对象。
     *
     * <p>注册时会扫描该对象中所有标注了 {@link EventListener} 的方法，
     * 并为每个方法创建对应的 {@link EventInvoker}。
     *
     * @param <L>       监听器类型
     * @param listener  监听器对象，不能为 null
     */
    <L> void register(@Nonnull L listener);

    /**
     * 注销一个已注册的监听器对象。
     *
     * @param <L>       监听器类型
     * @param listener  要注销的监听器对象，不能为 null
     */
    <L> void unregister(@Nonnull L listener);

    /**
     * 注销所有已注册的监听器。
     */
    void unregisterAll();

    /**
     * 创建并返回一个事件发布器。
     *
     * <p>返回的 {@link EventPublisher} 包含当前所有已注册监听器的调用器。
     * 注意：默认实现每次调用都会创建新的发布器实例。
     *
     * @return 事件发布器
     */
    @Nonnull
    EventPublisher publisher();
}
