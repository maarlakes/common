package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public final class Tokens {
    private Tokens() {
    }

    public static <T extends ExpirationAppToken<?, ?>> boolean isExpired(@Nonnull T token) {
        return token.getExpirationTime().isBefore(LocalDateTime.now());
    }

    public static <T extends ExpirationAppToken<K, V>, K, V> CompletionStage<? extends T> autoRefreshAsync(
            @Nonnull RefreshableTokenRepository<T, K, V> repository,
            @Nonnull CompletionStage<? extends T> tokenFuture) {
        return tokenFuture.thenCompose(token -> {
            final CompletableFuture<? extends T> future;
            if (isExpired(token)) {
                future = (CompletableFuture<? extends T>) repository.refreshAsync(token);
            } else {
                future = CompletableFuture.completedFuture(token);
            }
            return future;
        });
    }

    public static TokenException newTokenException(@Nonnull Throwable exception) {
        if (exception instanceof TokenException) {
            return (TokenException) exception;
        }
        return new TokenException(exception.getMessage(), exception);
    }
}
