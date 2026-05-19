package cn.maarlakes.common.token.access.weixin;

import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.Request;
import cn.maarlakes.common.token.access.AppId;
import jakarta.annotation.Nonnull;

public class DefaultWeixinTokenProvider extends AbstractWeixinTokenProvider {

    private final String accessTokenUrl;
    private final WeixinSecretMapper secretMapper;

    public DefaultWeixinTokenProvider(@Nonnull HttpClient httpClient, WeixinSecretMapper secretMapper) {
        this(httpClient, 0, secretMapper, "https://api.weixin.qq.com/cgi-bin/token");
    }

    public DefaultWeixinTokenProvider(@Nonnull HttpClient httpClient, WeixinSecretMapper secretMapper, String accessTokenUrl) {
        this(httpClient, 0, secretMapper, accessTokenUrl);
    }

    public DefaultWeixinTokenProvider(@Nonnull HttpClient httpClient, int retryCount, WeixinSecretMapper secretMapper) {
        this(httpClient, retryCount, secretMapper, "https://api.weixin.qq.com/cgi-bin/token");
    }

    public DefaultWeixinTokenProvider(@Nonnull HttpClient httpClient, int retryCount, WeixinSecretMapper secretMapper, String accessTokenUrl) {
        super(httpClient, retryCount);
        this.accessTokenUrl = accessTokenUrl;
        this.secretMapper = secretMapper;
    }

    @Nonnull
    @Override
    protected Request buildRequest(@Nonnull AppId appId) {
        return Request.builder()
                .get(this.accessTokenUrl)
                .addQueryParam("grant_type", "client_credential")
                .addQueryParam("appid", appId.getAppId())
                .addQueryParam("secret", this.secretMapper.getSecret(appId.getAppId()))
                .build();
    }
}
