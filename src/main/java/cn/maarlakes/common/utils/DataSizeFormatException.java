package cn.maarlakes.common.utils;

/**
 * @author linjpxc
 */
public class DataSizeFormatException extends IllegalArgumentException {
    private static final long serialVersionUID = 3296581293523710782L;

    public DataSizeFormatException() {
    }

    public DataSizeFormatException(String s) {
        super(s);
    }

    public DataSizeFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataSizeFormatException(Throwable cause) {
        super(cause);
    }
}
