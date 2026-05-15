package cn.maarlakes.common.chain;

import jakarta.annotation.Nonnull;

public interface ChainInvocationFactory {

    @Nonnull
    <H, R> ChainInvoker<H, R> create(@Nonnull Class<H> type, @Nonnull H[] handlers);
}
