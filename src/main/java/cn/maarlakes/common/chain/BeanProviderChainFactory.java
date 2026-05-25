package cn.maarlakes.common.chain;

import cn.maarlakes.common.factory.bean.BeanProvider;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;

/**
 * 通过 {@link BeanProvider} 发现处理器的责任链工厂。
 *
 * <p>允许传入自定义的 {@link BeanProvider} 实例，适用于需要特定 Bean 查找策略的场景。
 *
 * @author linjpxc
 */
public class BeanProviderChainFactory extends AbstractChainFactory {

    private static final Logger log = LoggerFactory.getLogger(BeanProviderChainFactory.class);

    @Nonnull
    protected final BeanProvider provider;

    /**
     * @param provider 用于查找处理器的 Bean 提供者
     */
    public BeanProviderChainFactory(@Nonnull BeanProvider provider) {
        this.provider = provider;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <H> H[] createHandlers(@Nonnull Class<H> type) {
        final H[] handlers = this.provider.getBeans(type).stream()
                .toArray(length -> (H[]) Array.newInstance(type, length));
        log.debug("通过BeanProvider发现 {} 个处理器: type={}", handlers.length, type.getName());
        return handlers;
    }
}
