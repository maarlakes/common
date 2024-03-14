package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public class MemoryExpirationTokenRepository<T extends ExpirationAppToken<A, V>, A, V> extends MemoryCacheableAppTokenRepository<T, A, V> implements ExpirationTokenRepository<T, A, V> {

    public MemoryExpirationTokenRepository(@Nonnull TokenFactory<T, A, V> tokenFactory) {
        super(tokenFactory);
    }

    @Nonnull
    @Override
    public CompletionStage<Void> removeExpiredTokenAsync() {
        return this.getExpiredTokensAsync().thenAccept(tokens -> tokens.forEach(token -> this.cacheTokens.remove(token.getAppId(), token)));
    }

    @Nonnull
    @Override
    public CompletionStage<T> refreshAsync(@Nonnull T token) {
        this.cacheTokens.remove(token.getAppId(), token);
        return this.getTokenAsync(token.getAppId());
    }
}
