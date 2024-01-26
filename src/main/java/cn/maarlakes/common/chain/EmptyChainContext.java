package cn.maarlakes.common.chain;

import cn.maarlakes.common.tuple.KeyValuePair;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author linjpxc
 */
public final class EmptyChainContext<H, R> implements ChainContext<H, R> {

    private EmptyChainContext() {
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <H, R> EmptyChainContext<H, R> getInstance() {
        return (EmptyChainContext<H, R>) Helper.INSTANCE;
    }

    @Override
    public R result() {
        return null;
    }

    @Nonnull
    @Override
    public List<KeyValuePair<H, R>> results() {
        return Collections.emptyList();
    }

    @Override
    public boolean addResult(@Nonnull H handler, R result) {
        return true;
    }

    @Override
    public R firstResult() {
        return null;
    }

    @Override
    public KeyValuePair<H, R> first() {
        return null;
    }

    @Nonnull
    @Override
    public Optional<KeyValuePair<H, R>> firstOptional() {
        return Optional.empty();
    }

    @Override
    public Optional<R> firstResultOptional() {
        return Optional.empty();
    }

    @Override
    public R lastResult() {
        return null;
    }

    @Nonnull
    @Override
    public Optional<R> lastResultOptional() {
        return Optional.empty();
    }

    @Override
    public KeyValuePair<H, R> last() {
        return null;
    }

    @Nonnull
    @Override
    public Optional<KeyValuePair<H, R>> lastOptional() {
        return Optional.empty();
    }

    private static final class Helper {
        public static final EmptyChainContext<?, ?> INSTANCE = new EmptyChainContext<>();
    }
}
