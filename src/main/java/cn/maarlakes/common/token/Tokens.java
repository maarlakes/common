package cn.maarlakes.common.token;

import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * Token 模块的静态工具类，提供过期判断、异步阻塞等待、异常解包等通用能力。
 *
 * <p>本类不可实例化，所有方法均为静态方法。
 *
 * @author linjpxc
 */
public final class Tokens {
    private Tokens() {
    }

    /**
     * 判断带过期时间的 Token 是否已过期。
     *
     * <p>通过比较 {@link ExpirationAppToken#getExpiresAt()} 与 {@link Instant#now()} 来判断，
     * 当过期时间严格早于当前时间时视为已过期。
     *
     * @param token 待判断的 Token
     * @param <T>   Token 类型
     * @return 如果已过期返回 {@code true}，否则返回 {@code false}
     */
    public static <T extends ExpirationAppToken<?, ?>> boolean isExpired(@Nonnull T token) {
        return token.getExpiresAt().isBefore(Instant.now());
    }

    /**
     * 将异常解包为 {@link TokenException}。
     *
     * <p>处理逻辑：
     * <ol>
     *   <li>如果异常本身是 {@link TokenException}，直接返回</li>
     *   <li>如果是 {@link CompletionException} 或 {@link ExecutionException}，递归解包到根本原因</li>
     *   <li>其他异常包装为新的 {@link TokenException}</li>
     * </ol>
     *
     * @param exception 原始异常
     * @return 解包后的 TokenException
     */
    public static TokenException newTokenException(@Nonnull Throwable exception) {
        if (exception instanceof TokenException) {
            return (TokenException) exception;
        }
        Throwable cause = exception;
        while (cause instanceof CompletionException || cause instanceof ExecutionException) {
            if (cause.getCause() == null || cause.getCause() == cause) {
                break;
            }
            cause = cause.getCause();
        }
        if (cause instanceof TokenException) {
            return (TokenException) cause;
        }
        return new TokenException(cause.getMessage(), cause);
    }

    /**
     * 阻塞等待异步操作完成并返回结果。
     *
     * <p>调用 {@link CompletionStage#toCompletableFuture()} 后通过 {@code get()} 阻塞等待。
     * 如果线程被中断，会恢复中断状态并抛出 {@link TokenException}。
     * 其他异常通过 {@link #newTokenException(Throwable)} 解包后抛出。
     *
     * @param stage 异步操作
     * @param <T>   返回值类型
     * @return 异步操作的结果
     * @throws TokenException 如果等待过程中发生异常
     */
    public static <T> T join(@Nonnull CompletionStage<T> stage) {
        try {
            return stage.toCompletableFuture().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw newTokenException(e);
        } catch (Exception e) {
            throw newTokenException(e);
        }
    }
}
