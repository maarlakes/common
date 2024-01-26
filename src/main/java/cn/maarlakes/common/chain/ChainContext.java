package cn.maarlakes.common.chain;

import cn.maarlakes.common.tuple.KeyValuePair;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Optional;

/**
 * @author linjpxc
 */
public interface ChainContext<H, R> {

    R result();

    @Nonnull
    List<KeyValuePair<H, R>> results();

    boolean addResult(@Nonnull H handler, R result);

    default R firstResult() {
        return this.firstResultOptional().orElse(null);
    }

    default Optional<R> firstResultOptional() {
        return this.firstOptional().map(KeyValuePair::value);
    }

    default KeyValuePair<H, R> first() {
        return this.firstOptional().orElse(null);
    }

    @Nonnull
    default Optional<KeyValuePair<H, R>> firstOptional() {
        final List<KeyValuePair<H, R>> results = this.results();
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(results.get(0));
    }

    default R lastResult() {
        return this.lastResultOptional().orElse(null);
    }

    @Nonnull
    default Optional<R> lastResultOptional() {
        return this.lastOptional().map(KeyValuePair::value);
    }

    default KeyValuePair<H, R> last() {
        return this.lastOptional().orElse(null);
    }

    @Nonnull
    default Optional<KeyValuePair<H, R>> lastOptional() {
        final List<KeyValuePair<H, R>> results = this.results();
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(results.get(results.size() - 1));
    }
}
