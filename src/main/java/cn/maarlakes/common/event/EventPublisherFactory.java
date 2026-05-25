package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * 事件发布器工厂，用于创建 {@link EventPublisher} 实例。
 *
 * @author linjpxc
 * @see DefaultEventPublisherFactory
 */
public interface EventPublisherFactory {

    /**
     * 创建一个包含指定分发器和调用器列表的事件发布器。
     *
     * @param dispatcher 事件分发器
     * @param invokers   所有已注册的事件调用器列表
     * @return 新创建的事件发布器
     */
    @Nonnull
    EventPublisher createPublisher(@Nonnull EventDispatcher dispatcher, @Nonnull List<? extends EventInvoker> invokers);
}
