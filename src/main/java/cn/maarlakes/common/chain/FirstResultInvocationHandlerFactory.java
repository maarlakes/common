package cn.maarlakes.common.chain;

import cn.maarlakes.common.utils.ArrayUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 「首个非空结果」调用处理器工厂。
 *
 * <p>按序调用所有处理器，遇到第一个返回非 {@code null} 的结果时立即停止后续调用并返回该结果。
 * 适用于只需要一个处理器响应的链式场景（如策略选择、优先级处理）。
 *
 * @author linjpxc
 */
public class FirstResultInvocationHandlerFactory implements InvocationHandlerFactory {

    private static final Logger log = LoggerFactory.getLogger(FirstResultInvocationHandlerFactory.class);

    private final boolean isReverse;

    /**
     * @param isReverse 是否逆序执行处理器（true 时低优先级优先）
     */
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

        private static final Logger log = LoggerFactory.getLogger(FirstResultInvocationHandler.class);

        FirstResultInvocationHandler(@Nonnull Class<H> type, @Nonnull H[] handlers) {
            super(type, handlers);
        }

        @Override
        protected Object handleInvocation(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) throws Throwable {
            log.debug("链式调用(首个结果策略): method={}, handler数={}", method.getName(), this.handlers.length);
            try {
                for (int i = 0; i < this.handlers.length; i++) {
                    final H handler = this.handlers[i];
                    log.trace("调用处理器 [{}/{}]: {}", i + 1, this.handlers.length, handler.getClass().getName());
                    final Object result = method.invoke(handler, args);
                    if (result != null) {
                        log.debug("处理器 {} 返回非空结果, 短路剩余 {} 个处理器", handler.getClass().getSimpleName(),
                                this.handlers.length - i - 1);
                        return result;
                    }
                    log.trace("处理器 {} 返回null, 继续下一个", handler.getClass().getSimpleName());
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            log.debug("所有处理器均返回null: method={}", method.getName());
            return null;
        }
    }
}
