package cn.maarlakes.common.chain;

import cn.maarlakes.common.function.Function1;
import cn.maarlakes.common.tuple.KeyValuePair;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author linjpxc
 */
public class DefaultChainContext<H, R> implements ChainContext<H, R> {
    protected final List<KeyValuePair<H, R>> results;
    protected final Function1<KeyValuePair<H, R>, Boolean> resultHandler;

    public DefaultChainContext() {
        this(new LinkedList<>(), r -> true);
    }

    public DefaultChainContext(@Nonnull List<KeyValuePair<H, R>> results) {
        this(results, r -> true);
    }

    public DefaultChainContext(@Nonnull Function1<KeyValuePair<H, R>, Boolean> resultHandler) {
        this(new LinkedList<>(), resultHandler);
    }

    public DefaultChainContext(@Nonnull List<KeyValuePair<H, R>> results, @Nonnull Function1<KeyValuePair<H, R>, Boolean> resultHandler) {
        this.results = results;
        this.resultHandler = resultHandler;
    }

    @Override
    public R result() {
        return this.lastResult();
    }

    @Nonnull
    @Override
    public List<KeyValuePair<H, R>> results() {
        return Collections.unmodifiableList(this.results);
    }

    @Override
    public boolean addResult(@Nonnull H handler, R result) {
        final KeyValuePair<H, R> pair = new KeyValuePair<>(handler, result);
        this.results.add(pair);
        return resultHandler.applyUnchecked(pair);
    }
}
