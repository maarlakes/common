package cn.maarlakes.common.token.access;

import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.Request;
import cn.maarlakes.common.http.Response;
import cn.maarlakes.common.token.TokenException;
import cn.maarlakes.common.token.Tokens;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * 基于 HTTP 的 Access Token 提供者抽象基类，使用模板方法模式将 Token 获取流程标准化。
 *
 * <p>核心流程：
 * <ol>
 *   <li>子类通过 {@link #buildRequest(AppId)} 构建具体的 HTTP 请求</li>
 *   <li>基类发送 HTTP 请求获取响应</li>
 *   <li>子类通过 {@link #parseToken(AppId, Response, Instant)} 解析 HTTP 响应为 AccessToken</li>
 *   <li>如果解析失败或返回 null，触发重试（最多 {@link #retryCount} 次）</li>
 * </ol>
 *
 * <p>重试机制：当 {@code retryCount > 0} 时，解析异常或响应无效（返回 null）会自动重试，
 * 直到达到最大重试次数后抛出最终的异常。
 *
 * @author linjpxc
 */
public abstract class AbstractAccessTokenProvider implements AccessTokenProvider {
    private static final Logger log = LoggerFactory.getLogger(AbstractAccessTokenProvider.class);

    /** HTTP 客户端，用于发送 Token 请求 */
    protected final HttpClient httpClient;

    /** 最大重试次数（不包含首次请求） */
    protected final int retryCount;

    /**
     * 使用默认重试次数（0，不重试）构造。
     *
     * @param httpClient HTTP 客户端
     */
    protected AbstractAccessTokenProvider(@Nonnull HttpClient httpClient) {
        this(httpClient, 0);
    }

    /**
     * 使用自定义重试次数构造。
     *
     * @param httpClient HTTP 客户端
     * @param retryCount 最大重试次数
     */
    protected AbstractAccessTokenProvider(@Nonnull HttpClient httpClient, int retryCount) {
        this.httpClient = httpClient;
        this.retryCount = retryCount;
    }

    @Nonnull
    @Override
    public CompletionStage<AccessToken> createToken(@Nonnull AppId appId) {
        return this.createToken(appId, 0);
    }

    /**
     * 构建获取 Token 的 HTTP 请求。由子类实现，定义具体的请求 URL、参数和 HTTP 方法。
     *
     * @param appId 应用标识
     * @return 构建好的 HTTP 请求
     */
    @Nonnull
    protected abstract Request buildRequest(@Nonnull AppId appId);

    /**
     * 解析 HTTP 响应为 AccessToken。由子类实现，定义具体的响应解析逻辑。
     *
     * @param appId    应用标识
     * @param response HTTP 响应
     * @param now      发起请求时的时间戳，用于计算过期时间
     * @return 解析出的 AccessToken，如果响应无效可返回 null（将触发重试）
     */
    protected abstract AccessToken parseToken(@Nonnull AppId appId, @Nonnull Response response, @Nonnull Instant now);

    /**
     * 内部实现：发起 HTTP 请求并解析响应，支持递归重试。
     *
     * @param appId   应用标识
     * @param current 当前已执行的请求次数（从 0 开始）
     * @return 异步返回 AccessToken
     */
    private CompletionStage<AccessToken> createToken(@Nonnull AppId appId, int current) {
        final Instant now = Instant.now();
        if (log.isDebugEnabled()) {
            log.debug("开始获取 Access Token：appId={}，第 {} 次请求", appId, current + 1);
        }
        return this.httpClient.execute(this.buildRequest(appId))
                .thenCompose(response -> {
                    if (log.isTraceEnabled()) {
                        log.trace("收到 Token 响应：appId={}，HTTP 状态码={}", appId, response.getStatusCode());
                    }
                    final AccessToken token;
                    try {
                        token = this.parseToken(appId, response, now);
                    } catch (Exception e) {
                        log.warn("解析 Token 响应失败：appId={}，第 {} 次请求，原因：{}", appId, current + 1, e.getMessage());
                        return this.retryOrFail(appId, current, e);
                    }
                    if (token != null) {
                        log.info("Access Token 获取成功：appId={}，过期时间={}", appId, token.getExpiresAt());
                        return CompletableFuture.completedFuture(token);
                    }
                    return this.retryOrFail(appId, current,
                            new TokenException("无法获取Token。HttpStatus:" + response.getStatusCode() + ", Description:" + response.getStatusText() + ", Body:" + response.getBody().asText()));
                });
    }

    /**
     * 重试或最终失败。
     *
     * <p>如果当前请求次数未达到 {@link #retryCount}，则递归调用 {@link #createToken} 重试；
     * 否则将异常包装为 {@link TokenException} 后返回失败的 Future。
     *
     * @param appId   应用标识
     * @param current 当前已执行的请求次数
     * @param error   导致重试的异常
     * @return 异步返回 AccessToken（重试成功）或异常失败
     */
    private CompletionStage<AccessToken> retryOrFail(@Nonnull AppId appId, int current, @Nonnull Throwable error) {
        if (current < this.retryCount) {
            log.warn("获取 Access Token 失败，准备重试：appId={}，第 {}/{} 次重试，原因：{}",
                    appId, current + 1, this.retryCount, error.getMessage());
            return this.createToken(appId, current + 1);
        }
        log.error("获取 Access Token 最终失败：appId={}，已重试 {} 次", appId, this.retryCount, error);
        final CompletableFuture<AccessToken> future = new CompletableFuture<>();
        future.completeExceptionally(Tokens.newTokenException(error));
        return future;
    }
}
