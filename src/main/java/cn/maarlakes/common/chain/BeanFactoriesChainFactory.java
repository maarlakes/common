package cn.maarlakes.common.chain;

import cn.maarlakes.common.factory.bean.BeanFactories;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Array;

/**
 * @author linjpxc
 */
public final class BeanFactoriesChainFactory extends AbstractChainFactory {

    @Override
    @SuppressWarnings("unchecked")
    protected <H> H[] createHandlers(@Nonnull Class<H> type) {
        return BeanFactories.getBeans(type).stream().toArray(length -> (H[]) Array.newInstance(type, length));
    }
}
