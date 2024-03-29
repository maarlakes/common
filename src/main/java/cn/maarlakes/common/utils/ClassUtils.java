package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * @author linjpxc
 */
public final class ClassUtils {
    private ClassUtils() {
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_MAP = new HashMap<>();

    static {
        PRIMITIVE_WRAPPER_MAP.put(Boolean.TYPE, Boolean.class);
        PRIMITIVE_WRAPPER_MAP.put(Byte.TYPE, Byte.class);
        PRIMITIVE_WRAPPER_MAP.put(Character.TYPE, Character.class);
        PRIMITIVE_WRAPPER_MAP.put(Short.TYPE, Short.class);
        PRIMITIVE_WRAPPER_MAP.put(Integer.TYPE, Integer.class);
        PRIMITIVE_WRAPPER_MAP.put(Long.TYPE, Long.class);
        PRIMITIVE_WRAPPER_MAP.put(Double.TYPE, Double.class);
        PRIMITIVE_WRAPPER_MAP.put(Float.TYPE, Float.class);
        PRIMITIVE_WRAPPER_MAP.put(Void.TYPE, Void.TYPE);
    }

    @Nonnull
    public static Constructor<?> getMatchingAccessibleDeclaredConstructor(Class<?> clazz, Object... args) {
        return getMatchingAccessibleDeclaredConstructor(clazz, parameterTypes(args));
    }

    @Nonnull
    public static Constructor<?> getMatchingAccessibleDeclaredConstructor(Class<?> clazz, Class<?>... parameterTypes) {
        Exception exception = null;
        try {
            return clazz.getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException e) {
            exception = e;
        }

        final Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        final Constructor<?> ctr = Arrays.stream(constructors)
                .filter(constructor -> matchTypes(constructor.getParameterTypes(), parameterTypes))
                .findFirst()
                .orElse(null);
        if (ctr != null) {
            return ctr;
        }
        throw new IllegalStateException(exception);
    }

    public static Class<?>[] parameterTypes(@Nonnull Object... args) {
        final Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            final Object arg = args[i];
            if (arg != null) {
                types[i] = arg.getClass();
            }
        }
        return types;
    }

    @SuppressWarnings("unchecked")
    public static <T> Class<T> loadClass(@Nonnull String className) {
        return (Class<T>) loadClassOptional(className).orElse(null);
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<Class<T>> loadClassOptional(@Nonnull String className) {
        try {
            return Optional.of((Class<T>) Class.forName(className));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> Class<T> loadClass(@Nonnull String className, boolean initialize, ClassLoader loader) {
        return (Class<T>) loadClassOptional(className, initialize, loader).orElse(null);
    }

    @SuppressWarnings({"unchecked"})
    public static <T> Optional<Class<T>> loadClassOptional(@Nonnull String className, boolean initialize, ClassLoader loader) {
        try {
            return Optional.of((Class<T>) Class.forName(className, initialize, loader));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    public static boolean hasClass(@Nonnull String className) {
        return hasClass(className, ClassUtils.class.getClassLoader());
    }

    public static boolean hasClass(@Nonnull String className, @Nonnull ClassLoader loader) {
        try {
            Class.forName(className, false, loader);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean matchTypes(Class<?>[] left, Class<?>[] right) {
        if (left.length != right.length) {
            return false;
        }
        for (int i = 0; i < left.length; i++) {
            final Class<?> l = wrapperType(left[i]);
            final Class<?> r = wrapperType(right[i]);
            if (r == null) {
                if (l.isPrimitive()) {
                    return false;
                }
            } else {
                if (!l.isAssignableFrom(r)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static Class<?> wrapperType(Class<?> type) {
        if (type != null && type.isPrimitive()) {
            final Class<?> tmp = PRIMITIVE_WRAPPER_MAP.get(type);
            if (tmp != null) {
                return tmp;
            }
        }
        return type;
    }
}
