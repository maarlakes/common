package cn.maarlakes.common.chain;

import cn.maarlakes.common.factory.bean.BeanFactories;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;

/**
 * 通过 {@link BeanFactories} 全局 Bean 容器发现处理器的责任链工厂。
 *
 * <p>使用 {@link BeanFactories#getBeans(Class)} 获取指定类型的所有 Bean 实例作为链处理器。
 *
 * @author linjpxc
 */
public final class BeanFactoriesChainFactory extends AbstractChainFactory {

    private static final Logger log = LoggerFactory.getLogger(BeanFactoriesChainFactory.class);

    @Override
    @SuppressWarnings("unchecked")
    protected <H> H[] createHandlers(@Nonnull Class<H> type) {
        final H[] handlers = BeanFactories.getBeans(type).stream()
                .toArray(length -> (H[]) Array.newInstance(type, length));
        log.debug("通过BeanFactories发现 {} 个处理器: type={}", handlers.length, type.getName());
        return handlers;
    }
}
