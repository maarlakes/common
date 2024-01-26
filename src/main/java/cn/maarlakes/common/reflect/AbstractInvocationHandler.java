package cn.maarlakes.common.reflect;

import jakarta.annotation.Nonnull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

/**
 * @author linjpxc
 */
public abstract class AbstractInvocationHandler implements InvocationHandler {
    protected static final Object[] EMPTY_ARGS = new Object[0];

    @Override
    public final Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (args == null) {
            args = EMPTY_ARGS;
        }
        if (args.length == 0 && "hashCode".equals(method.getName())) {
            return this.hashCode(proxy, method, args);
        }
        if (args.length == 0 && "toString".equals(method.getName())) {
            return this.toString(proxy, method, args);
        }
        if (args.length == 1 && "equals".equals(method.getName()) && method.getParameterTypes()[0] == Object.class) {
            return this.equals(proxy, method, args);
        }
        return this.handleInvocation(proxy, method, args);
    }

    protected abstract Object handleInvocation(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) throws Throwable;

    protected int hashCode(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) {
        return this.hashCode();
    }

    protected String toString(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) {
        return this.toString();
    }

    protected boolean equals(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) {
        Object arg = args[0];
        if (arg == null) {
            return false;
        }
        if (proxy == arg) {
            return true;
        }
        final Class<?> proxyClass = proxy.getClass();
        return proxyClass.isInstance(arg)
                || (Proxy.isProxyClass(arg.getClass()) && Arrays.equals(arg.getClass().getInterfaces(), proxyClass.getInterfaces()))
                && this.equals(Proxy.getInvocationHandler(proxy));
    }
}
