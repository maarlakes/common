package cn.maarlakes.common.token.access;

import cn.maarlakes.common.token.TokenException;

public class UnsupportedAppException extends TokenException {
    private static final long serialVersionUID = -7753187741470224815L;

    public UnsupportedAppException() {
    }

    public UnsupportedAppException(String message) {
        super(message);
    }

    public UnsupportedAppException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedAppException(Throwable cause) {
        super(cause);
    }

    public UnsupportedAppException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
