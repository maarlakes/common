package cn.maarlakes.common.token.access.weixin;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * {@link WeixinAppId} 的默认不可变实现。
 *
 * <p>通过包私有的构造函数和 {@link WeixinAppId#of} 静态工厂方法控制实例创建。
 *
 * <p>{@link #toString()} 返回 {@code "weixin@{appId}"} 格式，便于在日志中区分不同平台的 AppId。
 *
 * @author linjpxc
 */
class DefaultWeixinAppId implements WeixinAppId {

    private final String appId;

    DefaultWeixinAppId(@Nonnull String appId) {
        this.appId = Objects.requireNonNull(appId);
    }

    @Nonnull
    @Override
    public String getAppId() {
        return this.appId;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof WeixinAppId) {
            return Objects.equals(this.appId, ((WeixinAppId) o).getAppId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(appId);
    }

    @Override
    public String toString() {
        return "weixin@" + this.appId;
    }
}
