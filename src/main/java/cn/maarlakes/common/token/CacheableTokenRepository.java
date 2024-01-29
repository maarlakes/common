package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public interface CacheableTokenRepository<T extends AppToken<A, V>, A, V> extends TokenRepository<T, A, V> {

    @Nonnull
    CompletionStage<List<T>> getTokensAsync();

    @Nonnull
    default List<T> getTokens() {
        return this.getTokensAsync().toCompletableFuture().join();
    }

    @Nonnull
    CompletionStage<Void> clearAsync();

    default void clear() {
        this.clearAsync().toCompletableFuture().join();
    }

    @Nonnull
    default CompletionStage<Void> removeAsync(@Nonnull T token) {
        return this.removeAsync(token.getAppId());
    }

    default void remove(@Nonnull T token) {
        this.removeAsync(token).toCompletableFuture().join();
    }

    @Nonnull
    CompletionStage<Void> removeAsync(@Nonnull A appId);

    default void remove(@Nonnull A appId) {
        this.removeAsync(appId).toCompletableFuture().join();
    }
}
