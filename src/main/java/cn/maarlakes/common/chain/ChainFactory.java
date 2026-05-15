package cn.maarlakes.common.chain;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface ChainFactory {

    @Nonnull
    <H, R> Chain<H, R> createChain(@Nonnull Class<H> type, @Nonnull ChainInvocationFactory factory);

    @Nonnull
    <H> H createChain(@Nonnull Class<H> type, @Nonnull InvocationHandlerFactory factory);

    @Nonnull
    default <H> H createFirstResultChain(@Nonnull Class<H> type) {
        return this.createFirstResultChain(type, false);
    }

    @Nonnull
    default <H> H createFirstResultChain(@Nonnull Class<H> type, boolean isReverse) {
        return this.createChain(type, new FirstResultInvocationHandlerFactory(isReverse));
    }

    @Nonnull
    default <H> H createLastResultChain(@Nonnull Class<H> type) {
        return this.createLastResultChain(type, false);
    }

    @Nonnull
    default <H> H createLastResultChain(@Nonnull Class<H> type, boolean isReverse) {
        return this.createChain(type, new LastResultInvocationHandlerFactory(isReverse));
    }

    @Nonnull
    default <H> H createNoneResultChain(@Nonnull Class<H> type) {
        return this.createNoneResultChain(type, false);
    }

    @Nonnull
    default <H> H createNoneResultChain(@Nonnull Class<H> type, boolean isReverse) {
        return this.createChain(type, new NoneResultInvocationHandlerFactory(isReverse));
    }
}
