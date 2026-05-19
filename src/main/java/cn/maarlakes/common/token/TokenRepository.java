package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public interface TokenRepository<T extends AppToken<A, V>, A, V> {

    @Nonnull
    CompletionStage<T> getTokenAsync(@Nonnull A appId);

    @Nonnull
    default T getToken(@Nonnull A appId) {
       return Tokens.join(this.getTokenAsync(appId));
    }
}
