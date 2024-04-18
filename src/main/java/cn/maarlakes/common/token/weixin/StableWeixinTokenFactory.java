package cn.maarlakes.common.token.weixin;

import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClients;
import cn.maarlakes.common.http.Request;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public class StableWeixinTokenFactory extends AbstractWeixinTokenFactory {

    private final WeixinSecretMapper secretMapper;
    private final boolean forceRefresh;

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper) {
        this(mapper, HttpClients.defaultClient());
    }

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, boolean forceRefresh) {
        this(mapper, HttpClients.defaultClient(), forceRefresh);
    }

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull HttpClient httpClient) {
        this(mapper, httpClient, "https://api.weixin.qq.com/cgi-bin/stable_token", false);
    }

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull HttpClient httpClient, boolean forceRefresh) {
        this(mapper, httpClient, "https://api.weixin.qq.com/cgi-bin/stable_token", forceRefresh);
    }

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull String tokenUrl) {
        this(mapper, HttpClients.defaultClient(), tokenUrl, false);
    }

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull String tokenUrl, boolean forceRefresh) {
        this(mapper, HttpClients.defaultClient(), tokenUrl, forceRefresh);
    }

    public StableWeixinTokenFactory(@Nonnull WeixinSecretMapper mapper, @Nonnull HttpClient httpClient, @Nonnull String tokenUrl, boolean forceRefresh) {
        super(httpClient, tokenUrl);
        this.secretMapper = mapper;
        this.forceRefresh = forceRefresh;
    }

    @Override
    protected Request buildRequest(@Nonnull String appId) {
        final JSONObject json = new JSONObject();
        json.put("grant_type", "client_credential");
        json.put("appid", appId);
        json.put("secret", this.secretMapper.getSecret(appId));
        json.put("force_refresh", this.forceRefresh);
        return Request.builder()
                .post(this.url)
                .json(json.toString())
                .build();
    }
}
