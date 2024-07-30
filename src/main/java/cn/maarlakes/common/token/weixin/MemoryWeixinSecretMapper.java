package cn.maarlakes.common.token.weixin;

import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * @author linjpxc
 */
public class MemoryWeixinSecretMapper implements WeixinSecretMapper {

    private final Map<String, String> map;

    public MemoryWeixinSecretMapper(@Nonnull Map<String, String> map) {
        this.map = map;
    }

    @Override
    public String getSecret(@Nonnull String appId) {
        return this.map.get(appId);
    }
}
