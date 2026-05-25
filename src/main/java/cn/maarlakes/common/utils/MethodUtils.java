package cn.maarlakes.common.utils;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @author linjpxc
 */
public final class MethodUtils {
    private MethodUtils() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T invoke(Method method, Object object, Object... args) throws InvocationTargetException, IllegalAccessException {
        if (method == null) {
            throw new IllegalArgumentException("method is null");
        }
        if (!method.isAccessible()) {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                method.setAccessible(true);
                return null;
            });
        }
        return (T) method.invoke(object, args);
    }
}
