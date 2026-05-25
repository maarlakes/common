package cn.maarlakes.common.locks;

/**
 * 获取锁超时异常。
 *
 * <p>在以下场景中由 {@link AbstractLockMethodInterceptor#acquireLock} 抛出：</p>
 * <ul>
 *     <li>{@code waitTime == 0} 时，{@link Mutex#tryLock()} 返回 {@code false}（锁被其他线程持有，立即失败）</li>
 *     <li>{@code waitTime > 0} 时，{@link Mutex#tryLock(long, java.util.concurrent.TimeUnit)} 返回 {@code false}（超时未获取）</li>
 * </ul>
 *
 * <p>也由 {@link LockGuard#tryLock} 在超时时抛出。</p>
 *
 * @author linjpxc
 * @see AbstractLockMethodInterceptor
 * @see LockGuard
 */
public class SyncLockTimeoutException extends LockException {

    private static final long serialVersionUID = 2893218154878752269L;

    public SyncLockTimeoutException() {
    }

    public SyncLockTimeoutException(String message) {
        super(message);
    }

    public SyncLockTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public SyncLockTimeoutException(Throwable cause) {
        super(cause);
    }

    public SyncLockTimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
