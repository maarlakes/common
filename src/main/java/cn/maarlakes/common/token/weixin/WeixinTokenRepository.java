package cn.maarlakes.common.token.weixin;

import cn.maarlakes.common.token.ExpirationTokenRepository;

/**
 * @author linjpxc
 */
public interface WeixinTokenRepository extends ExpirationTokenRepository<WeixinToken, String, String> {
}
