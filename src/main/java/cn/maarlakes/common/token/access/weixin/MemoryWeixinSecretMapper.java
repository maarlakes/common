package cn.maarlakes.common.token.access.weixin;

import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * 基于内存 Map 的微信密钥映射实现。
 *
 * <p>使用一个简单的 {@link Map} 存储 AppID 到 AppSecret 的映射关系，
 * 适用于密钥数量有限且不需要动态更新的场景。
 *
 * <p>示例用法：
 * <pre>{@code
 * WeixinSecretMapper mapper = new MemoryWeixinSecretMapper(
 *     ImmutableMap.of("wx1234", "secret1", "wx5678", "secret2")
 * );
 * }</pre>
 *
 * @author linjpxc
 */
public class MemoryWeixinSecretMapper implements WeixinSecretMapper {

    private final Map<String, String> map;

    /**
     * 使用指定的映射表构造。
     *
     * @param map AppID → AppSecret 的映射关系
     */
    public MemoryWeixinSecretMapper(@Nonnull Map<String, String> map) {
        this.map = map;
    }

    @Override
    public String getSecret(@Nonnull String appId) {
        return this.map.get(appId);
    }
}
