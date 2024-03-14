package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

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
    public static <T> T invoke(@Nonnull Method method, Object object, Object... args) throws InvocationTargetException, IllegalAccessException {
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            method.setAccessible(true);
            return null;
        });
        return (T) method.invoke(object, args);
    }
}
