package cn.maarlakes.common.token.access;

import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class DefaultAccessTokenFactory implements AccessTokenFactory {

    private final List<? extends AccessTokenProvider> providers;

    public DefaultAccessTokenFactory(@Nonnull List<AccessTokenProvider> providers) {
        this.providers = new ArrayList<>(providers);
    }


    @Nonnull
    @Override
    public CompletionStage<AccessToken> createToken(@Nonnull AppId appId) {
        for (AccessTokenProvider provider : this.providers) {
            if (provider.supported(appId)) {
                return provider.createToken(appId);
            }
        }
        final CompletableFuture<AccessToken> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedAppException("不支持的App: " + appId.getClass() + ", appId: " + appId.getAppId()));
        return future;
    }
}
