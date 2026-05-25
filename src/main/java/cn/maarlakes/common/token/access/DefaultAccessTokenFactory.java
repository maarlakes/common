package cn.maarlakes.common.token.access;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 默认的 Access Token 工厂实现，通过遍历注册的 {@link AccessTokenProvider} 列表进行路由。
 *
 * <p>路由逻辑：遍历所有 Provider，调用 {@link AccessTokenProvider#supported(AppId)} 判断是否支持，
 * 使用第一个支持的 Provider 创建 Token。如果没有任何 Provider 支持，抛出 {@link UnsupportedAppException}。
 *
 * <p>Provider 的顺序决定了优先级，排在前面的 Provider 优先被使用。
 *
 * @author linjpxc
 */
public class DefaultAccessTokenFactory implements AccessTokenFactory {
    private static final Logger log = LoggerFactory.getLogger(DefaultAccessTokenFactory.class);

    private final List<? extends AccessTokenProvider> providers;

    /**
     * 使用 Provider 列表构造。
     *
     * @param providers Token 提供者列表，顺序决定优先级
     */
    public DefaultAccessTokenFactory(@Nonnull List<AccessTokenProvider> providers) {
        this.providers = new ArrayList<>(providers);
    }


    /**
     * 为指定应用创建 Access Token，自动路由到支持该 AppId 的 Provider。
     *
     * @param appId 应用标识
     * @return 异步返回创建好的 AccessToken
     * @throws UnsupportedAppException 如果没有任何 Provider 支持该 AppId
     */
    @Nonnull
    @Override
    public CompletionStage<AccessToken> createToken(@Nonnull AppId appId) {
        for (AccessTokenProvider provider : this.providers) {
            if (provider.supported(appId)) {
                if (log.isDebugEnabled()) {
                    log.debug("匹配到 Token Provider：appId={}，provider={}", appId, provider.getClass().getSimpleName());
                }
                return provider.createToken(appId);
            }
        }
        log.warn("没有可用的 Token Provider 能处理该应用：appId={}，appIdType={}", appId, appId.getClass().getName());
        final CompletableFuture<AccessToken> future = new CompletableFuture<>();
        future.completeExceptionally(new UnsupportedAppException("不支持的App: " + appId.getClass() + ", appId: " + appId.getAppId()));
        return future;
    }
}
