package cn.maarlakes.common.event;

/**
 * 事件总线异常，用于封装事件分发和监听器调用过程中的错误。
 *
 * <p>所有事件处理过程中的运行时异常（包括通过反射调用监听方法时抛出的
 * {@link java.lang.reflect.InvocationTargetException}）都会被解包后包装为本异常。
 *
 * @author linjpxc
 */
public class EventException extends RuntimeException {
    private static final long serialVersionUID = 1120623879956398998L;

    public EventException() {
    }

    public EventException(String message) {
        super(message);
    }

    public EventException(String message, Throwable cause) {
        super(message, cause);
    }

    public EventException(Throwable cause) {
        super(cause);
    }

    public EventException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
