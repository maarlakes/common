package cn.maarlakes.common.chain;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface ChainFactory {

    @Nonnull
    <H, R> H createChain(@Nonnull Class<H> type, @Nonnull ChainContext<H, R> context);

    @Nonnull
    default <H> H createEmptyContextChain(@Nonnull Class<H> type) {
        return this.createChain(type, EmptyChainContext.getInstance());
    }

    @Nonnull
    default <H> H createFirstContextChain(@Nonnull Class<H> type) {
        return this.createChain(type, new FirstChainContext<>());
    }

    @Nonnull
    <H, R> H createReverseChain(@Nonnull Class<H> type, @Nonnull ChainContext<H, R> context);

    @Nonnull
    default <H> H createEmptyContextReverseChain(@Nonnull Class<H> type) {
        return this.createReverseChain(type, EmptyChainContext.getInstance());
    }

    @Nonnull
    default <H> H createFirstContextReverseChain(@Nonnull Class<H> type) {
        return this.createReverseChain(type, new FirstChainContext<>());
    }
}
