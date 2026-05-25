package cn.maarlakes.common.token.access;

import cn.maarlakes.common.token.MemoryExpirationTokenRepository;
import jakarta.annotation.Nonnull;

/**
 * 基于内存的 Access Token 仓库实现。
 *
 * <p>继承 {@link MemoryExpirationTokenRepository} 的全部能力（缓存、过期检查、刷新），
 * 并将泛型特化为 {@link AccessToken} / {@link AppId} / {@link String}。
 *
 * <p>适用于单实例部署场景，Token 仅在当前 JVM 内存中缓存，进程重启后丢失。
 *
 * @author linjpxc
 */
public class MemoryAccessTokenRepository extends MemoryExpirationTokenRepository<AccessToken, AppId, String> implements AccessTokenRepository {
    public MemoryAccessTokenRepository(@Nonnull AccessTokenFactory tokenFactory) {
        super(tokenFactory);
    }
}
