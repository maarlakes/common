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
public class ContextChainInvocationHandler<H, R> extends AbstractInvocationHandler {

    protected final Class<H> type;
    protected final H[] handlers;
    protected final ChainContext<H, R> context;

    public ContextChainInvocationHandler(@Nonnull Class<H> type, @Nonnull H[] handlers, @Nonnull ChainContext<H, R> context) {
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
        if (obj instanceof ContextChainInvocationHandler) {
            final ContextChainInvocationHandler<H, R> that = (ContextChainInvocationHandler<H, R>) obj;
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
        return this.type + "$ContextChainProxy" + "@" + str.substring(Math.max(0, index + 1));
    }
}
