package cn.maarlakes.common.chain;

import cn.maarlakes.common.reflect.AbstractInvocationHandler;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public abstract class AbstractChainFactory implements ChainFactory {

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <H, R> H createChain(@Nonnull Class<H> type, @Nonnull ChainContext<H, R> context) {
        return (H) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new ChainInvocationHandler<>(type, this.createHandlers(type), context));
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <H, R> H createReverseChain(@Nonnull Class<H> type, @Nonnull ChainContext<H, R> context) {
        final H[] handlers = Arrays.stream(this.createHandlers(type)).collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
            Collections.reverse(list);
            return list.stream();
        })).toArray(length -> (H[]) Array.newInstance(type, length));
        return (H) Proxy.newProxyInstance(
                type.getClassLoader(), new Class[]{type},
                new ChainInvocationHandler<>(
                        type,
                        handlers,
                        context)
        );
    }

    protected abstract <H> H[] createHandlers(@Nonnull Class<H> type);

    @SuppressWarnings("unchecked")
    protected static class ChainInvocationHandler<H, R> extends AbstractInvocationHandler {

        private final Class<H> type;
        protected final H[] handlers;
        protected final ChainContext<H, R> context;

        protected ChainInvocationHandler(@Nonnull Class<H> type, @Nonnull H[] handlers, @Nonnull ChainContext<H, R> context) {
            this.type = type;
            this.handlers = handlers;
            this.context = context;
        }

        @Override
        protected Object handleInvocation(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) throws Throwable {
            try {
                for (H handler : this.handlers) {
                    if (!this.context.addResult(handler, (R) method.invoke(handler, args))) {
                        break;
                    }
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            return this.context.result();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof ChainInvocationHandler) {
                final ChainInvocationHandler<H, R> that = (ChainInvocationHandler<H, R>) obj;
                return this.type == that.type && Arrays.deepEquals(this.handlers, that.handlers);
            }
            return false;
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(type);
            result = 31 * result + Arrays.hashCode(handlers);
            return result;
        }

        @Override
        protected String toString(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) {
            final String str = super.toString();
            final int index = str.indexOf("@");
            return this.type + "$ChainProxy" + "@" + str.substring(Math.max(0, index + 1));
        }
    }
}
