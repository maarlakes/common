package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public interface ExpirationTokenRepository<T extends ExpirationAppToken<A, V>, A, V> extends RefreshableTokenRepository<T, A, V> {

    @Nonnull
    CompletionStage<Void> removeExpiredTokenAsync();

    default void removeExpiredToken() {
        this.removeExpiredTokenAsync().toCompletableFuture().join();
    }

    @Nonnull
    default CompletionStage<List<T>> getExpiredTokensAsync() {
        return this.getTokensAsync().thenApply(tokens -> tokens.stream().filter(Tokens::isExpired).collect(Collectors.toList()));
    }

    @Nonnull
    default List<T> getExpiredTokens() {
        return this.getExpiredTokensAsync().toCompletableFuture().join();
    }
}
