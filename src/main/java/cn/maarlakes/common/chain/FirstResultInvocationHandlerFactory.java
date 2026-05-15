package cn.maarlakes.common.chain;

import cn.maarlakes.common.utils.ArrayUtils;
import jakarta.annotation.Nonnull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class FirstResultInvocationHandlerFactory implements InvocationHandlerFactory {

    private final boolean isReverse;

    public FirstResultInvocationHandlerFactory(boolean isReverse) {
        this.isReverse = isReverse;
    }

    @Nonnull
    @Override
    public <H> InvocationHandler create(@Nonnull Class<H> type, @Nonnull H[] handlers) {
        handlers = handlers.clone();
        if (this.isReverse) {
            ArrayUtils.reverse(handlers);
        }
        return new FirstResultInvocationHandler<>(type, handlers);
    }

    private static final class FirstResultInvocationHandler<H> extends BasicInvocationHandler<H> {
        FirstResultInvocationHandler(@Nonnull Class<H> type, @Nonnull H[] handlers) {
            super(type, handlers);
        }

        @Override
        protected Object handleInvocation(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) throws Throwable {
            try {
                for (H handler : this.handlers) {
                    final Object result = method.invoke(handler, args);
                    if (result != null) {
                        return result;
                    }
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            return null;
        }
    }
}
