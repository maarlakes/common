package cn.maarlakes.common.chain;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;

/**
 * 责任链工厂的抽象基类。
 *
 * <p>实现了 {@link ChainFactory} 的两个 {@code createChain} 重载，将处理器发现逻辑
 * 延迟到子类的 {@link #createHandlers} 中。支持两种链创建方式：
 * <ul>
 *   <li>通过 {@link ChainInvocationFactory} 创建可获取每个处理器独立结果的 {@link Chain}</li>
 *   <li>通过 {@link InvocationHandlerFactory} 创建直接返回结果的动态代理</li>
 * </ul>
 *
 * @author linjpxc
 */
public abstract class AbstractChainFactory implements ChainFactory {

    private static final Logger log = LoggerFactory.getLogger(AbstractChainFactory.class);

    @Nonnull
    @Override
    public <H, R> Chain<H, R> createChain(@Nonnull Class<H> type, @Nonnull ChainInvocationFactory factory) {
        final H[] handlers = this.createHandlers(type);
        log.debug("创建责任链(调用器模式): type={}, handler数={}", type.getName(), handlers.length);
        return new DefaultChain<>(factory, type, handlers);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <H> H createChain(@Nonnull Class<H> type, @Nonnull InvocationHandlerFactory factory) {
        final H[] handlers = this.createHandlers(type);
        log.debug("创建责任链(代理模式): type={}, handler数={}", type.getName(), handlers.length);
        return (H) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, factory.create(type, handlers));
    }

    /**
     * 发现并返回指定类型的所有处理器实例。
     *
     * <p>子类通过此方法定义处理器来源（SPI、Bean 容器等）。
     *
     * @param type 处理器接口类型
     * @param <H>  处理器类型
     * @return 按优先级排序的处理器数组
     */
    protected abstract <H> H[] createHandlers(@Nonnull Class<H> type);

    private final class DefaultChain<H, R> implements Chain<H, R> {

        private final ChainInvocationFactory factory;
        private final Class<H> type;
        private final H[] handlers;

        private DefaultChain(ChainInvocationFactory factory, Class<H> type, H[] handlers) {
            this.factory = factory;
            this.type = type;
            this.handlers = handlers;
        }

        @Nonnull
        @Override
        public ChainInvoker<H, R> create() {
            return this.factory.create(this.type, this.handlers);
        }
    }
}
