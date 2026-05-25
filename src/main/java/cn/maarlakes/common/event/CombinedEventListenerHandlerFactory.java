package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 组合模式的事件监听器处理器工厂，将多个 {@link EventListenerHandlerFactory} 的结果合并。
 *
 * <p>适用于需要同时启用多种监听器发现机制的场景（如同时使用 SPI 加载和手动注册的处理器）。
 *
 * @author linjpxc
 */
public class CombinedEventListenerHandlerFactory implements EventListenerHandlerFactory {

    private static final Logger log = LoggerFactory.getLogger(CombinedEventListenerHandlerFactory.class);

    private final List<EventListenerHandlerFactory> factories;

    /**
     * 创建组合工厂。
     *
     * @param factories 要合并的工厂列表，不能为 null
     */
    public CombinedEventListenerHandlerFactory(@Nonnull List<EventListenerHandlerFactory> factories) {
        this.factories = factories;
    }

    @Nonnull
    @Override
    public List<EventListenerHandler> getListenerHandlers() {
        final List<EventListenerHandler> handlers = this.factories.stream()
                .flatMap(item -> item.getListenerHandlers().stream())
                .collect(Collectors.toList());
        if (log.isTraceEnabled()) {
            log.trace("组合工厂合并后共 {} 个事件监听器处理器", handlers.size());
        }
        return handlers;
    }
}
