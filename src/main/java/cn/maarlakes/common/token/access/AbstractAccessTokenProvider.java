package cn.maarlakes.common.token.access;

import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.Request;
import cn.maarlakes.common.http.Response;
import cn.maarlakes.common.token.TokenException;
import cn.maarlakes.common.token.Tokens;
import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public abstract class AbstractAccessTokenProvider implements AccessTokenProvider {

    protected final HttpClient httpClient;
    protected final int retryCount;

    protected AbstractAccessTokenProvider(@Nonnull HttpClient httpClient) {
        this(httpClient, 0);
    }

    protected AbstractAccessTokenProvider(@Nonnull HttpClient httpClient, int retryCount) {
        this.httpClient = httpClient;
        this.retryCount = retryCount;
    }

    @Nonnull
    @Override
    public CompletionStage<AccessToken> createToken(@Nonnull AppId appId) {
        return this.createToken(appId, 0);
    }

    @Nonnull
    protected abstract Request buildRequest(@Nonnull AppId appId);


    protected abstract AccessToken parseToken(@Nonnull AppId appId, @Nonnull Response response, @Nonnull Instant now);

    private CompletionStage<AccessToken> createToken(@Nonnull AppId appId, int current) {
        final Instant now = Instant.now();
        return this.httpClient.execute(this.buildRequest(appId))
                .thenCompose(response -> {
                    final AccessToken token;
                    try {
                        token = this.parseToken(appId, response, now);
                    } catch (Exception e) {
                        return this.retryOrFail(appId, current, e);
                    }
                    if (token != null) {
                        return CompletableFuture.completedFuture(token);
                    }
                    return this.retryOrFail(appId, current,
                            new TokenException("无法获取Token。HttpStatus:" + response.getStatusCode() + ", Description:" + response.getStatusText() + ", Body:" + response.getBody().asText()));
                });
    }

    private CompletionStage<AccessToken> retryOrFail(@Nonnull AppId appId, int current, @Nonnull Throwable error) {
        if (current < this.retryCount) {
            return this.createToken(appId, current + 1);
        }
        final CompletableFuture<AccessToken> future = new CompletableFuture<>();
        future.completeExceptionally(Tokens.newTokenException(error));
        return future;
    }
}
