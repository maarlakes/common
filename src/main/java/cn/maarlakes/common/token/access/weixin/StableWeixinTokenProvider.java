package cn.maarlakes.common.token.access.weixin;

import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.Request;
import cn.maarlakes.common.token.access.AppId;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Nonnull;

public class StableWeixinTokenProvider extends AbstractWeixinTokenProvider {

    private final String accessTokenUrl;
    private final boolean forceRefresh;
    private final WeixinSecretMapper secretMapper;

    public StableWeixinTokenProvider(boolean forceRefresh, @Nonnull HttpClient httpClient, WeixinSecretMapper secretMapper) {
        this(forceRefresh, httpClient, 0, secretMapper, "https://api.weixin.qq.com/cgi-bin/stable_token");
    }

    public StableWeixinTokenProvider(boolean forceRefresh, @Nonnull HttpClient httpClient, WeixinSecretMapper secretMapper, String accessTokenUrl) {
        this(forceRefresh, httpClient, 0, secretMapper, accessTokenUrl);
    }

    public StableWeixinTokenProvider(boolean forceRefresh, @Nonnull HttpClient httpClient, int retryCount, WeixinSecretMapper secretMapper) {
        this(forceRefresh, httpClient, retryCount, secretMapper, "https://api.weixin.qq.com/cgi-bin/stable_token");
    }

    public StableWeixinTokenProvider(boolean forceRefresh, @Nonnull HttpClient httpClient, int retryCount, WeixinSecretMapper secretMapper, String accessTokenUrl) {
        super(httpClient, retryCount);
        this.forceRefresh = forceRefresh;
        this.accessTokenUrl = accessTokenUrl;
        this.secretMapper = secretMapper;
    }

    @Nonnull
    @Override
    protected Request buildRequest(@Nonnull AppId appId) {
        final JSONObject json = new JSONObject();
        json.put("grant_type", "client_credential");
        json.put("appid", appId.getAppId());
        json.put("secret", this.secretMapper.getSecret(appId.getAppId()));
        json.put("force_refresh", this.forceRefresh);
        return Request.builder()
                .post(this.accessTokenUrl)
                .json(json.toString())
                .build();
    }
}
