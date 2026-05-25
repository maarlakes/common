package cn.maarlakes.common.locks;

/**
 * 锁相关异常的基类。
 *
 * <p>所有锁操作中抛出的运行时异常均继承此类。作为 {@link RuntimeException} 的子类，
 * 调用方不需要显式捕获，但应在适当层面（如 AOP 拦截器或全局异常处理器）进行处理。</p>
 *
 * <h3>子类</h3>
 * <ul>
 *     <li>{@link SyncLockTimeoutException} — 获取锁超时时抛出</li>
 *     <li>{@link MutexBindingException} — 同一个 key 混用同步锁和异步锁时抛出</li>
 * </ul>
 *
 * @author linjpxc
 * @see SyncLockTimeoutException
 * @see MutexBindingException
 */
public class LockException extends RuntimeException {

    private static final long serialVersionUID = -5869422953892293271L;

    public LockException() {
    }

    public LockException(String message) {
        super(message);
    }

    public LockException(String message, Throwable cause) {
        super(message, cause);
    }

    public LockException(Throwable cause) {
        super(cause);
    }

    public LockException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
