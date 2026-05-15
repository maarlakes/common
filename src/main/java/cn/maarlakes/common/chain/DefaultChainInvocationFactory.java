package cn.maarlakes.common.chain;

import cn.maarlakes.common.tuple.KeyValuePair;
import cn.maarlakes.common.utils.ArrayUtils;
import jakarta.annotation.Nonnull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class DefaultChainInvocationFactory implements ChainInvocationFactory {

    private final boolean isReverse;

    public DefaultChainInvocationFactory(boolean isReverse) {
        this.isReverse = isReverse;
    }

    @Nonnull
    @Override
    public <H, R> ChainInvoker<H, R> create(@Nonnull Class<H> type, @Nonnull H[] handlers) {
        handlers = handlers.clone();
        if (this.isReverse) {
            ArrayUtils.reverse(handlers);
        }
        return new DefaultChainInvoker<>(type, handlers);
    }

    @SuppressWarnings("unchecked")
    private static final class DefaultChainInvoker<H, R> implements ChainInvoker<H, R> {

        private final H invoker;
        private final DefaultInvocationHandler<H, R> handler;

        public DefaultChainInvoker(@Nonnull Class<H> type, @Nonnull H[] handlers) {
            this.handler = new DefaultInvocationHandler<>(type, handlers);
            this.invoker = (H) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, this.handler);
        }

        @Nonnull
        @Override
        public H instance() {
            return this.invoker;
        }

        @Nonnull
        @Override
        public List<KeyValuePair<H, R>> result() {
            return new ArrayList<>(this.handler.results);
        }
    }

    @SuppressWarnings("unchecked")
    private static final class DefaultInvocationHandler<H, R> extends BasicInvocationHandler<H> {

        private final List<KeyValuePair<H, R>> results = new ArrayList<>();

        DefaultInvocationHandler(@Nonnull Class<H> type, @Nonnull H[] handlers) {
            super(type, handlers);
        }

        @Override
        protected Object handleInvocation(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) throws Throwable {
            R result = null;
            try {
                this.results.clear();
                for (H handler : this.handlers) {
                    result = (R) method.invoke(handler, args);
                    this.results.add(new KeyValuePair<>(handler, result));
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            return result;
        }
    }
}
