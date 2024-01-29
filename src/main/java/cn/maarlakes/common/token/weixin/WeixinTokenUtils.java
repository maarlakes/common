package cn.maarlakes.common.token.weixin;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Nonnull;

import java.time.LocalDateTime;

/**
 * @author linjpxc
 */
public final class WeixinTokenUtils {
    private WeixinTokenUtils() {
    }

    @Nonnull
    public static WeixinToken toWeixinToken(@Nonnull String json, @Nonnull String appId, @Nonnull LocalDateTime now) {
        final JSONObject obj = JSON.parseObject(json);
        final String accessToken = obj.getString("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new WeixinTokenException(obj.getIntValue("errcode", -1), obj.getString("errmsg"));
        }

        return WeixinToken.of(appId, accessToken, now.plusSeconds(obj.getLongValue("expires_in")));
    }
}
