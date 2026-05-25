package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * 事件监听器处理器工厂，用于获取所有可用的 {@link EventListenerHandler} 实例。
 *
 * <p>支持通过 SPI 或手动组装的方式提供多个处理器。
 *
 * @author linjpxc
 * @see SpiEventListenerHandlerFactory
 * @see CombinedEventListenerHandlerFactory
 */
public interface EventListenerHandlerFactory {

    /**
     * 获取所有已注册的事件监听器处理器。
     *
     * @return 处理器列表，不会返回 null
     */
    @Nonnull
    List<EventListenerHandler> getListenerHandlers();
}
