package cn.maarlakes.common.chain;

import cn.maarlakes.common.utils.ArrayUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 「忽略返回值」调用处理器工厂。
 *
 * <p>按序调用所有处理器，始终返回 {@code null}。
 * 适用于纯副作用型操作（如通知、日志记录、事件广播），
 * 所有处理器都会被执行，不关心返回值。
 *
 * @author linjpxc
 */
public class NoneResultInvocationHandlerFactory implements InvocationHandlerFactory {

    private static final Logger log = LoggerFactory.getLogger(NoneResultInvocationHandlerFactory.class);

    private final boolean isReverse;

    /**
     * @param isReverse 是否逆序执行处理器
     */
    public NoneResultInvocationHandlerFactory(boolean isReverse) {
        this.isReverse = isReverse;
    }

    @Nonnull
    @Override
    public <H> InvocationHandler create(@Nonnull Class<H> type, @Nonnull H[] handlers) {
        handlers = handlers.clone();
        if (this.isReverse) {
            ArrayUtils.reverse(handlers);
        }

        return new NoneResultInvocationHandler<>(type, handlers);
    }

    private static final class NoneResultInvocationHandler<H> extends BasicInvocationHandler<H> {

        private static final Logger log = LoggerFactory.getLogger(NoneResultInvocationHandler.class);

        NoneResultInvocationHandler(@Nonnull Class<H> type, @Nonnull H[] handlers) {
            super(type, handlers);
        }

        @Override
        protected Object handleInvocation(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) throws Throwable {
            log.debug("链式调用(忽略结果策略): method={}, handler数={}", method.getName(), this.handlers.length);
            try {
                for (int i = 0; i < this.handlers.length; i++) {
                    final H handler = this.handlers[i];
                    log.trace("调用处理器 [{}/{}]: {}", i + 1, this.handlers.length, handler.getClass().getName());
                    method.invoke(handler, args);
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            return null;
        }
    }
}
