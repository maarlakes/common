package cn.maarlakes.common.function;

/**
 * @author linjpxc
 */
public class UncheckedException extends RuntimeException{
    private static final long serialVersionUID = 7232328903448613851L;

    public UncheckedException() {
    }

    public UncheckedException(String message) {
        super(message);
    }

    public UncheckedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UncheckedException(Throwable cause) {
        super(cause);
    }

    public UncheckedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
