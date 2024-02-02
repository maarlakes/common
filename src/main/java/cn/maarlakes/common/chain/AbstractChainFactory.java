package cn.maarlakes.common.chain;

import jakarta.annotation.Nonnull;

import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public abstract class AbstractChainFactory implements ChainFactory {

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <H, R> H createChain(@Nonnull Class<H> type, @Nonnull ChainContext<H, R> context) {
        return (H) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new ContextChainInvocationHandler<>(type, this.createHandlers(type), context));
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <H, R> H createReverseChain(@Nonnull Class<H> type, @Nonnull ChainContext<H, R> context) {
        return (H) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new ContextChainInvocationHandler<>(type, this.createReverseHandlers(type), context));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <H> H createNoneResultChain(@Nonnull Class<H> type) {
        return (H) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new NoneResultChainInvocationHandler<>(type, this.createHandlers(type)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <H> H createFirstResultChain(@Nonnull Class<H> type) {
        return (H) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new FirstResultChainInvocationHandler<>(type, this.createHandlers(type)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <H> H createNoneResultReverseChain(@Nonnull Class<H> type) {
        return (H) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new NoneResultChainInvocationHandler<>(type, this.createReverseHandlers(type)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <H> H createFirstResultReserveChain(@Nonnull Class<H> type) {
        return (H) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new FirstResultChainInvocationHandler<>(type, this.createReverseHandlers(type)));
    }

    protected abstract <H> H[] createHandlers(@Nonnull Class<H> type);

    @SuppressWarnings("unchecked")
    protected <H> H[] createReverseHandlers(@Nonnull Class<H> type) {
        return Arrays.stream(this.createHandlers(type))
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    Collections.reverse(list);
                    return list.stream();
                }))
                .toArray(length -> (H[]) Array.newInstance(type, length));
    }
}
