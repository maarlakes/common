package cn.maarlakes.common.token.access.weixin;

import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.Request;
import cn.maarlakes.common.token.access.AppId;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Nonnull;

/**
 * 微信 Stable Access Token 提供者，使用 POST 请求获取 Token。
 *
 * <p>对应微信 API：{@code POST https://api.weixin.qq.com/cgi-bin/stable_token}
 *
 * <p>与 {@link DefaultWeixinTokenProvider} 的区别：
 * <ul>
 *   <li>使用 POST 请求而非 GET，参数通过 JSON body 传递</li>
 *   <li>支持 {@code force_refresh} 参数，可以强制刷新微信服务端的 Token 缓存</li>
 *   <li>微信官方推荐用于高稳定性要求的场景</li>
 * </ul>
 *
 * <p>当 {@code forceRefresh} 设置为 {@code true} 时，每次调用都会强制微信服务端刷新 Token，
 * 即使当前 Token 仍在有效期内。适用于 Token 被其他业务方意外刷新后需要立即获取新 Token 的场景。
 *
 * @author linjpxc
 */
public class StableWeixinTokenProvider extends AbstractWeixinTokenProvider {

    /** Token API 地址 */
    private final String accessTokenUrl;

    /** 是否强制刷新 */
    private final boolean forceRefresh;

    /** 密钥映射器 */
    private final WeixinSecretMapper secretMapper;

    /**
     * 使用默认 API 地址构造。
     *
     * @param forceRefresh 是否强制刷新
     * @param httpClient   HTTP 客户端
     * @param secretMapper 密钥映射器
     */
    public StableWeixinTokenProvider(boolean forceRefresh, @Nonnull HttpClient httpClient, WeixinSecretMapper secretMapper) {
        this(forceRefresh, httpClient, 0, secretMapper, "https://api.weixin.qq.com/cgi-bin/stable_token");
    }

    /**
     * 使用自定义 API 地址构造。
     *
     * @param forceRefresh   是否强制刷新
     * @param httpClient     HTTP 客户端
     * @param secretMapper   密钥映射器
     * @param accessTokenUrl 自定义 Token API 地址
     */
    public StableWeixinTokenProvider(boolean forceRefresh, @Nonnull HttpClient httpClient, WeixinSecretMapper secretMapper, String accessTokenUrl) {
        this(forceRefresh, httpClient, 0, secretMapper, accessTokenUrl);
    }

    /**
     * 使用默认 API 地址和自定义重试次数构造。
     *
     * @param forceRefresh 是否强制刷新
     * @param httpClient   HTTP 客户端
     * @param retryCount   最大重试次数
     * @param secretMapper 密钥映射器
     */
    public StableWeixinTokenProvider(boolean forceRefresh, @Nonnull HttpClient httpClient, int retryCount, WeixinSecretMapper secretMapper) {
        this(forceRefresh, httpClient, retryCount, secretMapper, "https://api.weixin.qq.com/cgi-bin/stable_token");
    }

    /**
     * 使用自定义 API 地址和自定义重试次数构造。
     *
     * @param forceRefresh   是否强制刷新
     * @param httpClient     HTTP 客户端
     * @param retryCount     最大重试次数
     * @param secretMapper   密钥映射器
     * @param accessTokenUrl 自定义 Token API 地址
     */
    public StableWeixinTokenProvider(boolean forceRefresh, @Nonnull HttpClient httpClient, int retryCount, WeixinSecretMapper secretMapper, String accessTokenUrl) {
        super(httpClient, retryCount);
        this.forceRefresh = forceRefresh;
        this.accessTokenUrl = accessTokenUrl;
        this.secretMapper = secretMapper;
    }

    /**
     * 构建微信 Stable Token 请求（POST 方法，参数通过 JSON body 传递）。
     *
     * <p>JSON body 包含：grant_type、appid、secret、force_refresh 四个字段。
     */
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
