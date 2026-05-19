package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author linjpxc
 */
public class MemoryCacheableAppTokenRepository<T extends AppToken<A, V>, A, V> implements CacheableTokenRepository<T, A, V> {
    protected final ConcurrentMap<A, CompletableFuture<T>> cacheTokens = new ConcurrentHashMap<>();
    protected final TokenFactory<T, A, V> tokenFactory;

    public MemoryCacheableAppTokenRepository(@Nonnull TokenFactory<T, A, V> tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    @Nonnull
    @Override
    public CompletionStage<List<T>> getTokensAsync() {
        final List<T> tokens = new ArrayList<>();
        for (CompletableFuture<T> future : this.cacheTokens.values()) {
            if (future.isDone() && !future.isCompletedExceptionally()) {
                final T token = future.getNow(null);
                if (token != null) {
                    tokens.add(token);
                }
            }
        }
        return CompletableFuture.completedFuture(tokens);
    }

    @Nonnull
    @Override
    public CompletionStage<Void> clearAsync() {
        this.cacheTokens.clear();
        return CompletableFuture.completedFuture(null);
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeAsync(@Nonnull T token) {
        final CompletableFuture<T> future = this.cacheTokens.get(token.getAppId());
        if (future != null && future.isDone() && !future.isCompletedExceptionally()
                && token.equals(future.getNow(null))) {
            this.cacheTokens.remove(token.getAppId(), future);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeAsync(@Nonnull A appId) {
        this.cacheTokens.remove(appId);
        return CompletableFuture.completedFuture(null);
    }

    @Nonnull
    @Override
    public CompletionStage<T> getTokenAsync(@Nonnull A appId) {
        return this.cacheTokens.computeIfAbsent(appId, key -> {
            final CompletableFuture<T> future = new CompletableFuture<>();
            this.tokenFactory.createToken(key)
                    .thenAccept(future::complete)
                    .exceptionally(error -> {
                        this.cacheTokens.remove(key, future);
                        future.completeExceptionally(Tokens.newTokenException(error));
                        return null;
                    });
            return future;
        });
    }
}
