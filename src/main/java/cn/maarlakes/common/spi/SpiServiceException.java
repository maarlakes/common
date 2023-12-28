package cn.maarlakes.common.spi;

/**
 * @author linjpxc
 */
public class SpiServiceException extends RuntimeException {
    private static final long serialVersionUID = 3497200338955045456L;

    public SpiServiceException() {
    }

    public SpiServiceException(String message) {
        super(message);
    }

    public SpiServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public SpiServiceException(Throwable cause) {
        super(cause);
    }

    public SpiServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
