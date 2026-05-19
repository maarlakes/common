package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * @author linjpxc
 */
public final class Tokens {
    private Tokens() {
    }

    public static <T extends ExpirationAppToken<?, ?>> boolean isExpired(@Nonnull T token) {
        return token.getExpiresAt().isBefore(Instant.now());
    }

    public static TokenException newTokenException(@Nonnull Throwable exception) {
        if (exception instanceof TokenException) {
            return (TokenException) exception;
        }
        Throwable cause = exception;
        while (cause instanceof CompletionException || cause instanceof ExecutionException) {
            if (cause.getCause() == null || cause.getCause() == cause) {
                break;
            }
            cause = cause.getCause();
        }
        if (cause instanceof TokenException) {
            return (TokenException) cause;
        }
        return new TokenException(cause.getMessage(), cause);
    }

    public static <T> T join(@Nonnull CompletionStage<T> stage) {
        try {
            return stage.toCompletableFuture().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw newTokenException(e);
        } catch (Exception e) {
            throw newTokenException(e);
        }
    }
}
