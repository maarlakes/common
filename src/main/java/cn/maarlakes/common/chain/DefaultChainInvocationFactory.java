package cn.maarlakes.common.chain;

import cn.maarlakes.common.tuple.KeyValuePair;
import cn.maarlakes.common.utils.ArrayUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认链调用器工厂，收集每个处理器的独立执行结果。
 *
 * <p>与 {@link InvocationHandlerFactory} 的各种实现不同，此工厂返回的 {@link ChainInvoker}
 * 允许在代理方法调用后通过 {@link ChainInvoker#result()} 获取每个处理器与其返回值的配对列表。
 * 返回值为最后一个处理器的执行结果。
 *
 * @author linjpxc
 */
public class DefaultChainInvocationFactory implements ChainInvocationFactory {

    private static final Logger log = LoggerFactory.getLogger(DefaultChainInvocationFactory.class);

    private final boolean isReverse;

    /**
     * @param isReverse 是否逆序执行处理器
     */
    public DefaultChainInvocationFactory(boolean isReverse) {
        this.isReverse = isReverse;
    }

    @Nonnull
    @Override
    public <H, R> ChainInvoker<H, R> create(@Nonnull Class<H> type, @Nonnull H[] handlers) {
        handlers = handlers.clone();
        if (this.isReverse) {
            ArrayUtils.reverse(handlers);
        }
        log.debug("创建默认链调用器: type={}, handler数={}, reverse={}", type.getName(), handlers.length, this.isReverse);
        return new DefaultChainInvoker<>(type, handlers);
    }

    @SuppressWarnings("unchecked")
    private static final class DefaultChainInvoker<H, R> implements ChainInvoker<H, R> {

        private static final Logger log = LoggerFactory.getLogger(DefaultChainInvoker.class);

        private final H invoker;
        private final DefaultInvocationHandler<H, R> handler;

        public DefaultChainInvoker(@Nonnull Class<H> type, @Nonnull H[] handlers) {
            this.handler = new DefaultInvocationHandler<>(type, handlers);
            this.invoker = (H) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, this.handler);
        }

        @Nonnull
        @Override
        public H instance() {
            return this.invoker;
        }

        @Nonnull
        @Override
        public List<KeyValuePair<H, R>> result() {
            return new ArrayList<>(this.handler.results);
        }
    }

    @SuppressWarnings("unchecked")
    private static final class DefaultInvocationHandler<H, R> extends BasicInvocationHandler<H> {

        private static final Logger log = LoggerFactory.getLogger(DefaultInvocationHandler.class);

        /** 每次方法调用后清空并重新填充 */
        private final List<KeyValuePair<H, R>> results = new ArrayList<>();

        DefaultInvocationHandler(@Nonnull Class<H> type, @Nonnull H[] handlers) {
            super(type, handlers);
        }

        @Override
        protected Object handleInvocation(@Nonnull Object proxy, @Nonnull Method method, @Nonnull Object[] args) throws Throwable {
            log.debug("链式调用(收集结果): method={}, handler数={}", method.getName(), this.handlers.length);
            R result = null;
            try {
                this.results.clear();
                for (int i = 0; i < this.handlers.length; i++) {
                    final H handler = this.handlers[i];
                    log.trace("调用处理器 [{}/{}]: {}", i + 1, this.handlers.length, handler.getClass().getName());
                    result = (R) method.invoke(handler, args);
                    this.results.add(new KeyValuePair<>(handler, result));
                    log.trace("处理器 {} 返回: {}", handler.getClass().getSimpleName(), result);
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
            return result;
        }
    }
}
