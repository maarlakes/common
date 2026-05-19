package cn.maarlakes.common.token.access.weixin;

import cn.maarlakes.common.token.access.AccessToken;
import cn.maarlakes.common.token.access.AppId;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Nonnull;

import java.time.Instant;

public final class WeixinTokenUtils {
    private WeixinTokenUtils() {
    }

    @Nonnull
    public static AccessToken toWeixinToken(@Nonnull String json, @Nonnull AppId appId, @Nonnull Instant now) {
        final JSONObject obj = JSON.parseObject(json);
        final String accessToken = obj.getString("access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new WeixinTokenException(obj.getIntValue("errcode", -1), obj.getString("errmsg"));
        }

        final Long expiresIn = obj.getLong("expires_in");
        if (expiresIn == null || expiresIn <= 0) {
            throw new WeixinTokenException(obj.getIntValue("errcode", -1), obj.getString("errmsg"));
        }
        return AccessToken.of(appId, accessToken, now.plusSeconds(expiresIn));
    }
}
