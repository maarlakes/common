package cn.maarlakes.common.chain;

import cn.maarlakes.common.spi.SpiServiceLoader;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Array;

/**
 * @author linjpxc
 */
public final class SpiChainFactory extends AbstractChainFactory {

    private final ClassLoader loader;
    private final boolean isShared;

    public SpiChainFactory() {
        this(null, true);
    }

    public SpiChainFactory(ClassLoader loader) {
        this(loader, true);
    }

    public SpiChainFactory(boolean isShared) {
        this(null, isShared);
    }

    public SpiChainFactory(ClassLoader loader, boolean isShared) {
        this.loader = loader;
        this.isShared = isShared;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <H> H[] createHandlers(@Nonnull Class<H> type) {
        final ClassLoader classLoader = this.loader == null ? type.getClassLoader() : this.loader;
        return (this.isShared ? SpiServiceLoader.loadShared(type, classLoader) : SpiServiceLoader.load(type, classLoader))
                .stream().toArray(count -> (H[]) Array.newInstance(type, count));
    }
}
