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
        return Tokens.join(this.getTokensAsync());
    }

    @Nonnull
    CompletionStage<Void> clearAsync();

    default void clear() {
        Tokens.join(this.clearAsync());
    }

    @Nonnull
    default CompletionStage<Void> removeAsync(@Nonnull T token) {
        return this.removeAsync(token.getAppId());
    }

    default void remove(@Nonnull T token) {
        Tokens.join(this.removeAsync(token));
    }

    @Nonnull
    CompletionStage<Void> removeAsync(@Nonnull A appId);

    default void remove(@Nonnull A appId) {
        Tokens.join(this.removeAsync(appId));
    }
}
