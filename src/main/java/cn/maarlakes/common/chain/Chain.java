package cn.maarlakes.common.chain;

import jakarta.annotation.Nonnull;

public interface Chain<H, R> {

    @Nonnull
    ChainInvoker<H, R> create();
}
