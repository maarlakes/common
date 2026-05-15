package cn.maarlakes.common.chain;

import cn.maarlakes.common.reflect.AbstractInvocationHandler;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

public abstract class BasicInvocationHandler<H> extends AbstractInvocationHandler {

    protected final Class<H> type;
    protected final H[] handlers;

    protected BasicInvocationHandler(@Nonnull Class<H> type, @Nonnull H[] handlers) {
        this.type = type;
        this.handlers = handlers;
    }


    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BasicInvocationHandler) {
            final BasicInvocationHandler<?> that = (BasicInvocationHandler<?>) obj;
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
        return this.type.getName() + "$ChainProxy@" + Integer.toHexString(System.identityHashCode(this));
    }
}
