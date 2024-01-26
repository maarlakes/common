package cn.maarlakes.common.chain;

import cn.maarlakes.common.tuple.KeyValuePair;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * @author linjpxc
 */
public final class FirstChainContext<H, R> implements ChainContext<H, R> {

    private final List<KeyValuePair<H, R>> results = new LinkedList<>();
    private KeyValuePair<H, R> first = null;
    private KeyValuePair<H, R> last = null;

    @Override
    public R result() {
        return this.firstResult();
    }

    @Nonnull
    @Override
    public List<KeyValuePair<H, R>> results() {
        return Collections.unmodifiableList(results);
    }

    @Override
    public boolean addResult(@Nonnull H handler, R result) {
        final KeyValuePair<H, R> r = new KeyValuePair<>(handler, result);
        this.results.add(r);
        this.last = r;
        if (this.first == null && result != null) {
            this.first = r;
            return false;
        }
        return true;
    }

    @Nonnull
    @Override
    public Optional<KeyValuePair<H, R>> firstOptional() {
        return Optional.ofNullable(this.first);
    }

    @Nonnull
    @Override
    public Optional<KeyValuePair<H, R>> lastOptional() {
        return Optional.ofNullable(this.last);
    }
}
