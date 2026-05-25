package cn.maarlakes.common.chain;

import cn.maarlakes.common.utils.ArrayUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 「最后一个结果」调用处理器工厂。
 *
 * <p>按序调用所有处理器，始终返回最后一个处理器的执行结果。
 * 适用于后置处理器覆盖前置结果的链式场景（如配置覆盖、默认值回退）。
 *
 * @author linjpxc
 */
public class LastResultInvocationHandlerFactory implements InvocationHandlerFactory {

    private static final Logger log = LoggerFactory.getLogger(LastResultInvocationHandlerFactory.class);

    private final boolean isReverse;

    /**
     * @param isReverse 是否逆序执行处理器
     */
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

        private static final Logger log = LoggerFactory.getLogger(LastResultInvocationHandler.class);

        LastResultInvocationHandler(@Nonnull Class<H> type, @Nonnull H[] handlers) {
            super(type, handlers);
        }

        @Override
        protected Object handleInvocation(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) throws Throwable {
            log.debug("链式调用(末尾结果策略): method={}, handler数={}", method.getName(), this.handlers.length);
            Object result = null;
            try {
                for (int i = 0; i < this.handlers.length; i++) {
                    final H handler = this.handlers[i];
                    log.trace("调用处理器 [{}/{}]: {}", i + 1, this.handlers.length, handler.getClass().getName());
                    result = method.invoke(handler, args);
                    log.trace("处理器 {} 返回: {}", handler.getClass().getSimpleName(), result);
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            return result;
        }
    }
}
