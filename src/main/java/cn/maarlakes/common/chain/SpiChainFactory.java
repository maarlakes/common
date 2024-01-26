package cn.maarlakes.common.chain;

import cn.maarlakes.common.spi.SpiServiceLoader;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Array;
import java.util.stream.StreamSupport;

/**
 * @author linjpxc
 */
public final class SpiChainFactory extends AbstractChainFactory {

    private final boolean isShared;

    public SpiChainFactory() {
        this(true);
    }

    public SpiChainFactory(boolean isShared) {
        this.isShared = isShared;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <H> H[] createHandlers(@Nonnull Class<H> type) {
        return StreamSupport.stream((this.isShared ? SpiServiceLoader.loadShared(type, type.getClassLoader()) : SpiServiceLoader.load(type, type.getClassLoader())).spliterator(), false)
                .toArray(count -> (H[]) Array.newInstance(type, count));
    }
}
