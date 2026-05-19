package cn.maarlakes.common.token.access.weixin;

import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.Response;
import cn.maarlakes.common.token.TokenException;
import cn.maarlakes.common.token.access.AbstractAccessTokenProvider;
import cn.maarlakes.common.token.access.AccessToken;
import cn.maarlakes.common.token.access.AppId;
import jakarta.annotation.Nonnull;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

public abstract class AbstractWeixinTokenProvider extends AbstractAccessTokenProvider {

    protected AbstractWeixinTokenProvider(@Nonnull HttpClient httpClient) {
        super(httpClient);
    }

    protected AbstractWeixinTokenProvider(@Nonnull HttpClient httpClient, int retryCount) {
        super(httpClient, retryCount);
    }

    @Override
    protected AccessToken parseToken(@Nonnull AppId appId, @Nonnull Response response, @Nonnull Instant now) {
        if (response.getStatusCode() == 200) {
            return WeixinTokenUtils.toWeixinToken(response.getBody().asText(StandardCharsets.UTF_8), appId, now);
        }
        final String bodyText;
        try {
            bodyText = response.getBody().asText(StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new TokenException("网络请求错误，HttpStatus:" + response.getStatusCode() + ", Description:" + response.getStatusText());
        }
        throw new TokenException("网络请求错误，HttpStatus:" + response.getStatusCode() + ", Description:" + response.getStatusText() + ", Body:" + bodyText);
    }

    @Override
    public final boolean supported(@Nonnull AppId appId) {
        return appId instanceof WeixinAppId;
    }
}
