package cn.maarlakes.common.chain;

import jakarta.annotation.Nonnull;

import java.lang.reflect.Proxy;

/**
 * @author linjpxc
 */
public abstract class AbstractChainFactory implements ChainFactory {

    @Nonnull
    @Override
    public <H, R> Chain<H, R> createChain(@Nonnull Class<H> type, @Nonnull ChainInvocationFactory factory) {
        return new DefaultChain<>(factory, type);
    }


    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <H> H createChain(@Nonnull Class<H> type, @Nonnull InvocationHandlerFactory factory) {
        return (H) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, factory.create(type, this.createHandlers(type)));
    }

    protected abstract <H> H[] createHandlers(@Nonnull Class<H> type);

    private final class DefaultChain<H, R> implements Chain<H, R> {

        private final ChainInvocationFactory factory;
        private final Class<H> type;

        private DefaultChain(ChainInvocationFactory factory, Class<H> type) {
            this.factory = factory;
            this.type = type;
        }

        @Nonnull
        @Override
        public ChainInvoker<H, R> create() {
            return this.factory.create(this.type, createHandlers(this.type));
        }
    }
}
