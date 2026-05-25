package cn.maarlakes.common.token.access.weixin;

import jakarta.annotation.Nonnull;

/**
 * 微信密钥映射接口，根据 AppID 查找对应的 AppSecret。
 *
 * <p>微信 Token 获取接口需要 AppID 和 AppSecret 两个参数，
 * 此接口负责将 AppID 映射到对应的 AppSecret，解耦密钥管理与 Token 获取逻辑。
 *
 * <p>实现可以选择从内存、配置文件、数据库或密钥管理服务等不同来源获取密钥。
 *
 * @author linjpxc
 */
@FunctionalInterface
public interface WeixinSecretMapper {

    /**
     * 根据应用标识获取对应的 AppSecret。
     *
     * @param appId 微信 AppID
     * @return 对应的 AppSecret，如果未找到可能返回 null
     */
    String getSecret(@Nonnull String appId);
}
