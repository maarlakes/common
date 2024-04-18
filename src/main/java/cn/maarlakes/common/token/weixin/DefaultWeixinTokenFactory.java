package cn.maarlakes.common.token.weixin;

import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClients;
import cn.maarlakes.common.http.Request;
import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public class DefaultWeixinTokenFactory extends AbstractWeixinTokenFactory {

    private final WeixinSecretMapper secretMapper;

    public DefaultWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper) {
        this(mapper, HttpClients.defaultClient());
    }

    public DefaultWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull HttpClient httpClient) {
        this(mapper, httpClient, "https://api.weixin.qq.com/cgi-bin/token");
    }

    public DefaultWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull String tokenUrl) {
        this(mapper, HttpClients.defaultClient(), tokenUrl);
    }

    public DefaultWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull HttpClient httpClient, @Nonnull String tokenUrl) {
        super(httpClient, tokenUrl);
        this.secretMapper = mapper;
    }

    @Override
    protected Request buildRequest(@Nonnull String appId) {
        return Request.builder()
                .post(this.url)
                .addQueryParam("grant_type", "client_credential")
                .addQueryParam("appid", appId)
                .addQueryParam("secret", this.secretMapper.getSecret(appId))
                .build();
    }
}
