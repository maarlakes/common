package cn.maarlakes.common.chain;

import cn.maarlakes.common.utils.ArrayUtils;
import jakarta.annotation.Nonnull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class LastResultInvocationHandlerFactory implements InvocationHandlerFactory {

    private final boolean isReverse;

    public LastResultInvocationHandlerFactory(boolean isReverse) {
        this.isReverse = isReverse;
    }

    @Nonnull
    @Override
    public <H> InvocationHandler create(@Nonnull Class<H> type, @Nonnull H[] handlers) {
        handlers = handlers.clone();
        if (this.isReverse) {
            ArrayUtils.reverse(handlers);
        }
        return new LastResultInvocationHandler<>(type, handlers);
    }

    private static final class LastResultInvocationHandler<H> extends BasicInvocationHandler<H> {
        LastResultInvocationHandler(@Nonnull Class<H> type, @Nonnull H[] handlers) {
            super(type, handlers);
        }

        @Override
        protected Object handleInvocation(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) throws Throwable {
            Object result = null;
            try {
                for (H handler : this.handlers) {
                    result = method.invoke(handler, args);
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            return result;
        }
    }
}
