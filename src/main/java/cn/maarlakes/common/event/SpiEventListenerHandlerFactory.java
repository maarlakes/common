package cn.maarlakes.common.event;

import cn.maarlakes.common.spi.SpiServiceLoader;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 基于 SPI 的事件监听器处理器工厂，通过 {@link SpiServiceLoader} 加载 {@link EventListenerHandler} 实现。
 *
 * <p>支持两种加载模式：
 * <ul>
 *   <li><b>共享模式（shared）</b>：通过 {@code SpiServiceLoader.loadShared()} 加载，返回缓存的单例实例</li>
 *   <li><b>非共享模式</b>：通过 {@code SpiServiceLoader.load()} 加载，每次创建新实例</li>
 * </ul>
 *
 * @author linjpxc
 */
public final class SpiEventListenerHandlerFactory implements EventListenerHandlerFactory {

    private static final Logger log = LoggerFactory.getLogger(SpiEventListenerHandlerFactory.class);

    /** 类加载器，用于 SPI 服务发现 */
    private final ClassLoader loader;
    /** 是否使用共享模式（缓存单例） */
    private final boolean isShared;

    /**
     * 使用默认类加载器和共享模式创建工厂。
     */
    public SpiEventListenerHandlerFactory() {
        this(EventListenerHandler.class.getClassLoader(), true);
    }

    /**
     * 使用默认类加载器和指定的共享策略创建工厂。
     *
     * @param isShared 是否使用共享模式
     */
    public SpiEventListenerHandlerFactory(boolean isShared) {
        this(EventListenerHandler.class.getClassLoader(), isShared);
    }

    /**
     * 使用指定的类加载器和共享模式创建工厂。
     *
     * @param loader 类加载器
     */
    public SpiEventListenerHandlerFactory(@Nonnull ClassLoader loader) {
        this(loader, true);
    }

    /**
     * 使用指定的类加载器和共享策略创建工厂。
     *
     * @param loader   类加载器
     * @param isShared 是否使用共享模式（true 时返回缓存实例，false 时每次创建新实例）
     */
    public SpiEventListenerHandlerFactory(@Nonnull ClassLoader loader, boolean isShared) {
        this.loader = loader;
        this.isShared = isShared;
    }

    @Nonnull
    @Override
    public List<EventListenerHandler> getListenerHandlers() {
        final List<EventListenerHandler> handlers = (isShared ? SpiServiceLoader.loadShared(EventListenerHandler.class, this.loader)
                : SpiServiceLoader.load(EventListenerHandler.class, this.loader))
                .stream()
                .collect(Collectors.toList());
        if (log.isDebugEnabled()) {
            log.debug("通过 SPI 加载了 {} 个事件监听器处理器 [共享模式={}]", handlers.size(), isShared);
        }
        if (log.isTraceEnabled()) {
            for (EventListenerHandler handler : handlers) {
                log.trace("  已加载处理器: {}", handler.getClass().getName());
            }
        }
        return handlers;
    }
}
