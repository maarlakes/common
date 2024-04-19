package cn.maarlakes.common.locks;

/**
 * @author linjpxc
 */
public class SyncLockTimeoutException extends RuntimeException {
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
