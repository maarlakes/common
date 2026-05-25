package cn.maarlakes.common.chain;

import cn.maarlakes.common.spi.SpiServiceLoader;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;

/**
 * 通过 SPI（{@link SpiServiceLoader}）发现处理器的责任链工厂。
 *
 * <p>从 {@code META-INF/services/} 配置文件中加载指定接口的实现类作为链处理器。
 * 支持共享（单例缓存）和非共享两种加载模式。
 *
 * @author linjpxc
 */
public final class SpiChainFactory extends AbstractChainFactory {

    private static final Logger log = LoggerFactory.getLogger(SpiChainFactory.class);

    /** 指定的类加载器，为 null 时使用接口类型的类加载器 */
    private final ClassLoader loader;

    /** 是否使用共享（单例缓存）模式加载 SPI 服务 */
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
        log.debug("通过SPI加载处理器: type={}, shared={}", type.getName(), this.isShared);
        final H[] handlers = (this.isShared
                ? SpiServiceLoader.loadShared(type, classLoader)
                : SpiServiceLoader.load(type, classLoader))
                .stream().toArray(count -> (H[]) Array.newInstance(type, count));
        if (handlers.length == 0) {
            log.warn("未发现任何SPI处理器: type={}", type.getName());
        } else {
            log.debug("通过SPI发现 {} 个处理器: type={}", handlers.length, type.getName());
        }
        return handlers;
    }
}
