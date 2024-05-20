package cn.maarlakes.common.token.weixin;

import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.Request;
import cn.maarlakes.common.token.TokenException;
import cn.maarlakes.common.token.Tokens;
import jakarta.annotation.Nonnull;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public abstract class AbstractWeixinTokenFactory implements WeixinTokenFactory {

    protected final HttpClient httpClient;
    protected final String url;

    protected AbstractWeixinTokenFactory(@Nonnull HttpClient httpClient, @Nonnull String url) {
        this.httpClient = httpClient;
        this.url = url;
    }

    @Nonnull
    @Override
    public final CompletionStage<WeixinToken> createToken(@Nonnull String appId) {
        final LocalDateTime now = LocalDateTime.now();
        return this.httpClient.execute(this.buildRequest(appId))
                .thenApply(response -> {
                    if (response.getStatusCode() == 200) {
                        return WeixinTokenUtils.toWeixinToken(response.getBody().asText(StandardCharsets.UTF_8), appId, now);
                    }
                    throw new TokenException("网络请求错误，HttpStatus:" + response.getStatusCode() + ", Description:" + response.getStatusText());
                }).exceptionally(error -> {
                    throw Tokens.newTokenException(error);
                });
    }

    protected abstract Request buildRequest(@Nonnull String appId);
}
