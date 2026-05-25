package cn.maarlakes.common.token.access;

import cn.maarlakes.common.token.ExpirationTokenRepository;

/**
 * Access Token 仓库接口，泛型特化为 {@link AccessToken} 类型。
 *
 * <p>继承 {@link ExpirationTokenRepository} 的全部能力：缓存、刷新、过期管理。
 * 具体实现可选择内存（{@link MemoryAccessTokenRepository}）或 Redis（{@link RedissonAccessTokenRepository}）后端。
 *
 * @author linjpxc
 */
public interface AccessTokenRepository extends ExpirationTokenRepository<AccessToken, AppId, String> {
}
