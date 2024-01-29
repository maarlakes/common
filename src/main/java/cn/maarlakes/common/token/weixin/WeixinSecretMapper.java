package cn.maarlakes.common.token.weixin;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface WeixinSecretMapper {

    String getSecret(@Nonnull String appId);
}
