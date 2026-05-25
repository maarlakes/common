package cn.maarlakes.common.token.access.weixin;

import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.Request;
import cn.maarlakes.common.token.access.AppId;
import jakarta.annotation.Nonnull;

/**
 * 标准微信 Access Token 提供者，使用 GET 请求获取 Token。
 *
 * <p>对应微信 API：{@code GET https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET}
 *
 * <p>适用于大多数微信公众平台的 Token 获取场景。如果需要更稳定的 Token 获取（避免微信服务端缓存不一致），
 * 可使用 {@link StableWeixinTokenProvider} 替代。
 *
 * <p>支持自定义 Token API 地址，适用于代理或私有部署场景。
 *
 * @author linjpxc
 */
public class DefaultWeixinTokenProvider extends AbstractWeixinTokenProvider {

    /** Token API 地址 */
    private final String accessTokenUrl;

    /** 密钥映射器，根据 AppID 查找 AppSecret */
    private final WeixinSecretMapper secretMapper;

    /**
     * 使用默认 API 地址（https://api.weixin.qq.com/cgi-bin/token）和不重试构造。
     *
     * @param httpClient  HTTP 客户端
     * @param secretMapper 密钥映射器
     */
    public DefaultWeixinTokenProvider(@Nonnull HttpClient httpClient, WeixinSecretMapper secretMapper) {
        this(httpClient, 0, secretMapper, "https://api.weixin.qq.com/cgi-bin/token");
    }

    /**
     * 使用自定义 API 地址和不重试构造。
     *
     * @param httpClient    HTTP 客户端
     * @param secretMapper  密钥映射器
     * @param accessTokenUrl 自定义 Token API 地址
     */
    public DefaultWeixinTokenProvider(@Nonnull HttpClient httpClient, WeixinSecretMapper secretMapper, String accessTokenUrl) {
        this(httpClient, 0, secretMapper, accessTokenUrl);
    }

    /**
     * 使用默认 API 地址和自定义重试次数构造。
     *
     * @param httpClient  HTTP 客户端
     * @param retryCount  最大重试次数
     * @param secretMapper 密钥映射器
     */
    public DefaultWeixinTokenProvider(@Nonnull HttpClient httpClient, int retryCount, WeixinSecretMapper secretMapper) {
        this(httpClient, retryCount, secretMapper, "https://api.weixin.qq.com/cgi-bin/token");
    }

    /**
     * 使用自定义 API 地址和自定义重试次数构造。
     *
     * @param httpClient    HTTP 客户端
     * @param retryCount    最大重试次数
     * @param secretMapper  密钥映射器
     * @param accessTokenUrl 自定义 Token API 地址
     */
    public DefaultWeixinTokenProvider(@Nonnull HttpClient httpClient, int retryCount, WeixinSecretMapper secretMapper, String accessTokenUrl) {
        super(httpClient, retryCount);
        this.accessTokenUrl = accessTokenUrl;
        this.secretMapper = secretMapper;
    }

    /**
     * 构建微信标准 Token 请求（GET 方法，参数通过 query string 传递）。
     */
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
