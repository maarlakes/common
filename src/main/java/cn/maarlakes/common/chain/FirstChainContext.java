package cn.maarlakes.common.chain;

import cn.maarlakes.common.tuple.KeyValuePair;
import jakarta.annotation.Nonnull;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * @author linjpxc
 */
public final class FirstChainContext<H, R> extends DefaultChainContext<H, R> {

    public FirstChainContext() {
        this(new LinkedList<>());
    }

    public FirstChainContext(@Nonnull List<KeyValuePair<H, R>> results) {
        super(results, Objects::isNull);
    }
}
