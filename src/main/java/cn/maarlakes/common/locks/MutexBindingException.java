package cn.maarlakes.common.locks;

/**
 * 同一个 key 混用同步锁和异步锁时抛出。
 *
 * <p>{@link LockClient} 要求同一个 key 只能绑定一种锁类型（同步或异步）。
 * 一旦通过 {@link LockClient#getMutex} 或 {@link LockClient#getAsyncMutex}
 * 为某个 key 创建了锁实例，后续对该 key 调用另一种方法将抛出此异常。</p>
 *
 * <p>此限制存在的原因：同步锁和异步锁的底层实现不同（如 {@link java.util.concurrent.locks.ReentrantLock}
 * vs {@link java.util.concurrent.Semaphore}），混用会破坏互斥语义。</p>
 *
 * @author linjpxc
 * @see LockClient
 * @see SystemLockClient
 */
public class MutexBindingException extends LockException {

    private static final long serialVersionUID = -4872629067979226615L;

    public MutexBindingException() {
    }

    public MutexBindingException(String message) {
        super(message);
    }

    public MutexBindingException(String message, Throwable cause) {
        super(message, cause);
    }

    public MutexBindingException(Throwable cause) {
        super(cause);
    }

    public MutexBindingException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
