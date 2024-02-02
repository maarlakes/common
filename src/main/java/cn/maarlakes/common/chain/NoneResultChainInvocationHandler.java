package cn.maarlakes.common.chain;

import cn.maarlakes.common.reflect.AbstractInvocationHandler;
import jakarta.annotation.Nonnull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author linjpxc
 */
@SuppressWarnings("unchecked")
public class NoneResultChainInvocationHandler<H> extends AbstractInvocationHandler {

    protected final Class<H> type;
    protected final H[] handlers;

    public NoneResultChainInvocationHandler(@Nonnull Class<H> type, @Nonnull H[] handlers) {
        this.type = type;
        this.handlers = handlers;
    }

    @Override
    protected Object handleInvocation(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) throws Throwable {
        try {
            for (H handler : this.handlers) {
                method.invoke(handler, args);
            }
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NoneResultChainInvocationHandler) {
            final NoneResultChainInvocationHandler<H> that = (NoneResultChainInvocationHandler<H>) obj;
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
        return this.type + "$NoneResultChainProxy" + "@" + str.substring(Math.max(0, index + 1));
    }
}
