package cn.maarlakes.common.token.weixin;

import cn.maarlakes.common.token.MemoryExpirationTokenRepository;
import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public class MemoryWeixinTokenRepository extends MemoryExpirationTokenRepository<WeixinToken, String, String> implements WeixinTokenRepository {
    public MemoryWeixinTokenRepository(@Nonnull WeixinTokenFactory factory) {
        super(factory);
    }
}
