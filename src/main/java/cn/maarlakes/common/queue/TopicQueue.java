package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/**
 * @author linjpxc
 */
public interface TopicQueue<T> extends Iterable<T> {

    @Nonnull
    String name();

    int size();

    CompletionStage<Integer> sizeAsync();

    boolean isEmpty();

    CompletionStage<Boolean> isEmptyAsync();

    boolean offer(@Nonnull T value);

    CompletionStage<Boolean> offerAsync(@Nonnull T value);

    void clear();

    CompletionStage<Void> clearAsync();

    boolean remove(@Nonnull T value);

    CompletionStage<Boolean> removeAsync(@Nonnull T value);

    boolean removeAll(@Nonnull Collection<? extends T> values);

    CompletionStage<Boolean> removeAllAsync(@Nonnull Collection<? extends T> values);

    List<? extends T> removeIf(@Nonnull Predicate<T> predicate);

    boolean contains(@Nonnull T value);

    CompletionStage<Boolean> containsAsync(@Nonnull T value);

    void addListener(@Nonnull QueueListener<T> listener);

    void removeListener(@Nonnull QueueListener<T> listener);
}
