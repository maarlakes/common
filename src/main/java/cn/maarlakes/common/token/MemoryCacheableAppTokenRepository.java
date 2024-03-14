package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public class MemoryCacheableAppTokenRepository<T extends AppToken<A, V>, A, V> implements CacheableTokenRepository<T, A, V> {
    protected final ConcurrentMap<A, Object> cacheTokens = new ConcurrentHashMap<>();
    protected final TokenFactory<T, A, V> tokenFactory;

    public MemoryCacheableAppTokenRepository(@Nonnull TokenFactory<T, A, V> tokenFactory) {
        this.tokenFactory = tokenFactory;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public CompletionStage<List<T>> getTokensAsync() {
        return CompletableFuture.completedFuture(this.cacheTokens.values().stream().filter(item -> !(item instanceof CompletionStage<?>))
                .map(item -> (T) item)
                .collect(Collectors.toList()));
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
        this.cacheTokens.remove(token.getAppId(), token);
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
    @SuppressWarnings("unchecked")
    public CompletionStage<T> getTokenAsync(@Nonnull A appId) {
        final Object result = this.cacheTokens.computeIfAbsent(appId, key -> this.tokenFactory.createToken(appId)
                .thenApply(token -> {
                    this.cacheTokens.put(key, token);
                    return token;
                }).exceptionally(error -> {
                    this.cacheTokens.remove(key);
                    throw Tokens.newTokenException(error);
                }));
        if (result instanceof CompletionStage) {
            return (CompletionStage<T>) result;
        }
        return CompletableFuture.completedFuture((T) result);
    }
}
