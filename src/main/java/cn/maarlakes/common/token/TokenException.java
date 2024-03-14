package cn.maarlakes.common.token;

/**
 * @author linjpxc
 */
public class TokenException extends RuntimeException {
    private static final long serialVersionUID = 6186180909649477937L;

    public TokenException() {
    }

    public TokenException(String message) {
        super(message);
    }

    public TokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenException(Throwable cause) {
        super(cause);
    }

    public TokenException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
