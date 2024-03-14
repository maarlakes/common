package cn.maarlakes.common.chain;

import cn.maarlakes.common.factory.bean.BeanProvider;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Array;

/**
 * @author linjpxc
 */
public class BeanProviderChainFactory extends AbstractChainFactory {

    @Nonnull
    protected final BeanProvider provider;

    public BeanProviderChainFactory(@Nonnull BeanProvider provider) {
        this.provider = provider;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <H> H[] createHandlers(@Nonnull Class<H> type) {
        return this.provider.getBeans(type).stream().toArray(length -> (H[]) Array.newInstance(type, length));
    }
}
