package cn.maarlakes.common.token.access.weixin;

import cn.maarlakes.common.token.access.AccessToken;
import cn.maarlakes.common.token.access.AppId;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * 微信 Token 响应解析工具类，将微信 API 返回的 JSON 转换为 {@link AccessToken}。
 *
 * <p>微信 Token API 成功时返回的 JSON 格式：
 * <pre>{@code {"access_token":"ACCESS_TOKEN","expires_in":7200}}</pre>
 *
 * <p>失败时返回的 JSON 格式：
 * <pre>{@code {"errcode":40013,"errmsg":"invalid appid"}}</pre>
 *
 * <p>本工具类不负责 HTTP 请求，仅处理 JSON 解析逻辑。
 *
 * @author linjpxc
 */
public final class WeixinTokenUtils {
    private static final Logger log = LoggerFactory.getLogger(WeixinTokenUtils.class);

    private WeixinTokenUtils() {
    }

    /**
     * 将微信 API 返回的 JSON 字符串解析为 {@link AccessToken}。
     *
     * <p>解析逻辑：
     * <ol>
     *   <li>尝试提取 {@code access_token} 字段，如果为空则抛出 {@link WeixinTokenException}</li>
     *   <li>尝试提取 {@code expires_in} 字段（秒），如果无效则抛出 {@link WeixinTokenException}</li>
     *   <li>基于 {@code now + expires_in} 计算过期时间</li>
     * </ol>
     *
     * @param json  微信 API 返回的 JSON 字符串
     * @param appId 应用标识
     * @param now   请求发起时间，用于计算过期时间
     * @return 解析成功的 AccessToken
     * @throws WeixinTokenException 如果微信返回了错误码或响应格式异常
     */
    @Nonnull
    public static AccessToken toWeixinToken(@Nonnull String json, @Nonnull AppId appId, @Nonnull Instant now) {
        final JSONObject obj = JSON.parseObject(json);
        if (obj == null) {
            throw new WeixinTokenException(-1, "响应解析失败");
        }
        final String accessToken = obj.getString("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            final int errcode = obj.getIntValue("errcode", -1);
            final String errmsg = obj.getString("errmsg");
            log.warn("微信 Token 响应中缺少 access_token：appId={}，errcode={}，errmsg={}", appId, errcode, errmsg);
            throw new WeixinTokenException(errcode, errmsg);
        }

        final Long expiresIn = obj.getLong("expires_in");
        if (expiresIn == null || expiresIn <= 0) {
            final int errcode = obj.getIntValue("errcode", -1);
            final String errmsg = obj.getString("errmsg");
            log.warn("微信 Token 响应中 expires_in 无效：appId={}，errcode={}，errmsg={}", appId, errcode, errmsg);
            throw new WeixinTokenException(errcode, errmsg);
        }
        if (log.isDebugEnabled()) {
            log.debug("微信 Token 解析成功：appId={}，有效期 {} 秒", appId, expiresIn);
        }
        return AccessToken.of(appId, accessToken, now.plusSeconds(expiresIn));
    }
}
