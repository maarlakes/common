package cn.maarlakes.common.locks;

/**
 * 同一个 key 混用同步锁和异步锁时抛出。
 *
 * @author linjpxc
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
