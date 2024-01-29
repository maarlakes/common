package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public interface TokenFactory<T extends AppToken<A, V>, A, V> {

    @Nonnull
    CompletionStage<T> createToken(@Nonnull A appId);
}
