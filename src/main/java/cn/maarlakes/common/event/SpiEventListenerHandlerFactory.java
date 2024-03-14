package cn.maarlakes.common.event;

import cn.maarlakes.common.spi.SpiServiceLoader;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public final class SpiEventListenerHandlerFactory implements EventListenerHandlerFactory {

    private final ClassLoader loader;
    private final boolean isShared;

    public SpiEventListenerHandlerFactory() {
        this(EventListenerHandler.class.getClassLoader(), true);
    }

    public SpiEventListenerHandlerFactory(boolean isShared) {
        this(EventListenerHandler.class.getClassLoader(), isShared);
    }

    public SpiEventListenerHandlerFactory(@Nonnull ClassLoader loader) {
        this(loader, true);
    }

    public SpiEventListenerHandlerFactory(@Nonnull ClassLoader loader, boolean isShared) {
        this.loader = loader;
        this.isShared = isShared;
    }

    @Nonnull
    @Override
    public List<EventListenerHandler> getListenerHandlers() {
        return (isShared ? SpiServiceLoader.loadShared(EventListenerHandler.class, this.loader)
                : SpiServiceLoader.load(EventListenerHandler.class, this.loader))
                .stream()
                .collect(Collectors.toList());
    }
}
