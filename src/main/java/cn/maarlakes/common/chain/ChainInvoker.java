package cn.maarlakes.common.chain;

import cn.maarlakes.common.tuple.KeyValuePair;
import jakarta.annotation.Nonnull;

import java.util.List;

public interface ChainInvoker<H, R> {

    @Nonnull
    H instance();

    @Nonnull
    List<KeyValuePair<H, R>> result();
}
