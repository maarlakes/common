package cn.maarlakes.common.token.access.weixin;

import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.Response;
import cn.maarlakes.common.token.TokenException;
import cn.maarlakes.common.token.access.AbstractAccessTokenProvider;
import cn.maarlakes.common.token.access.AccessToken;
import cn.maarlakes.common.token.access.AppId;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * 微信 Token 提供者的抽象基类，处理微信 API 的通用响应解析和 Provider 匹配逻辑。
 *
 * <p>职责：
 * <ul>
 *   <li>{@link #supported(AppId)} — 仅支持 {@link WeixinAppId} 类型的应用标识</li>
 *   <li>{@link #parseToken(AppId, Response, Instant)} — HTTP 200 时使用 {@link WeixinTokenUtils} 解析 JSON；非 200 时抛出异常</li>
 * </ul>
 *
 * <p>子类只需实现 {@link #buildRequest(AppId)} 即可完成具体的 Token 获取逻辑。
 *
 * @author linjpxc
 */
public abstract class AbstractWeixinTokenProvider extends AbstractAccessTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(AbstractWeixinTokenProvider.class);

    protected AbstractWeixinTokenProvider(@Nonnull HttpClient httpClient) {
        super(httpClient);
    }

    protected AbstractWeixinTokenProvider(@Nonnull HttpClient httpClient, int retryCount) {
        super(httpClient, retryCount);
    }

    /**
     * 解析微信 Token API 的 HTTP 响应。
     *
     * <p>HTTP 200 时使用 {@link WeixinTokenUtils#toWeixinToken} 解析 JSON；
     * 非 200 时抛出包含状态码、描述和响应体的异常。
     *
     * @param appId    应用标识
     * @param response HTTP 响应
     * @param now      请求发起时间
     * @return 解析出的 AccessToken
     * @throws TokenException 如果 HTTP 状态码非 200 或响应解析失败
     */
    @Override
    protected AccessToken parseToken(@Nonnull AppId appId, @Nonnull Response response, @Nonnull Instant now) {
        if (response.getStatusCode() == 200) {
            return WeixinTokenUtils.toWeixinToken(response.getBody().asText(StandardCharsets.UTF_8), appId, now);
        }
        final String bodyText;
        try {
            bodyText = response.getBody().asText(StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("微信 Token 请求失败且响应体读取异常：appId={}，HTTP 状态码={}，描述={}",
                    appId, response.getStatusCode(), response.getStatusText());
            throw new TokenException("网络请求错误，HttpStatus:" + response.getStatusCode() + ", Description:" + response.getStatusText());
        }
        log.warn("微信 Token 请求失败：appId={}，HTTP 状态码={}，描述={}，响应体={}",
                appId, response.getStatusCode(), response.getStatusText(), bodyText);
        throw new TokenException("网络请求错误，HttpStatus:" + response.getStatusCode() + ", Description:" + response.getStatusText() + ", Body:" + bodyText);
    }

    /**
     * 判断是否支持给定的应用标识。仅支持 {@link WeixinAppId} 类型。
     *
     * @param appId 应用标识
     * @return 如果是 {@link WeixinAppId} 类型返回 {@code true}
     */
    @Override
    public final boolean supported(@Nonnull AppId appId) {
        return appId instanceof WeixinAppId;
    }
}
