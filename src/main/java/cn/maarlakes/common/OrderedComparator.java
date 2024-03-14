package cn.maarlakes.common;

import cn.maarlakes.common.utils.ClassUtils;
import cn.maarlakes.common.utils.MethodUtils;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Comparator;

/**
 * @author linjpxc
 */
public class OrderedComparator implements Comparator<Object> {

    private static final Class<?> SPRING_ORDER_TYPE;

    static {
        SPRING_ORDER_TYPE = ClassUtils.loadClass("org.springframework.core.Ordered");
    }

    protected OrderedComparator() {
    }

    @Nonnull
    public static OrderedComparator getInstance() {
        return Helper.COMPARATOR;
    }

    @Override
    @SuppressWarnings({"unused", "rawtypes"})
    public int compare(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return 0;
        }
        if (obj1 == null) {
            return Integer.compare(Ordered.LOWEST, getOrder(obj2));
        }
        if (obj2 == null) {
            return Integer.compare(this.getOrder(obj1), Ordered.LOWEST);
        }
        final Class<?> obj1Type = obj1.getClass();
        final Class<?> obj2Type = obj2.getClass();
        final Class<Comparable> comparableType = Comparable.class;
        if (comparableType.isAssignableFrom(obj1Type) && comparableType.isAssignableFrom(obj2Type)) {
            try {
                final Method compareTo = Arrays.stream(obj1Type.getMethods())
                        .filter(method -> "compareTo".equals(method.getName()) && method.getParameterCount() == 1 && method.getReturnType() == int.class)
                        .filter(method -> method.getParameterTypes()[0].isAssignableFrom(obj2Type))
                        .findFirst().orElse(null);
                if (compareTo != null && compareTo.getParameterTypes()[0] != Object.class) {
                    return MethodUtils.invoke(compareTo, obj1, obj2);
                } else {
                    final Method compare = Arrays.stream(obj2Type.getMethods())
                            .filter(method -> "compareTo".equals(method.getName()) && method.getParameterCount() == 1 && method.getReturnType() == int.class)
                            .filter(method -> method.getParameterTypes()[0].isAssignableFrom(obj1Type))
                            .findFirst().orElse(null);
                    if (compare != null) {
                        return MethodUtils.invoke(compare, obj2, obj1);
                    } else if (compareTo != null) {
                        return MethodUtils.invoke(compareTo, obj1, obj2);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return Integer.compare(getOrder(obj1), getOrder(obj2));
    }

    protected int getOrder(Object obj) {
        if (obj == null) {
            return Ordered.LOWEST;
        }
        final Integer order = findOrder(obj);
        return order == null ? Ordered.LOWEST : order;
    }

    protected Integer findOrder(@Nonnull Object obj) {
        if (obj instanceof Ordered) {
            return ((Ordered) obj).order();
        }
        if (SPRING_ORDER_TYPE != null && SPRING_ORDER_TYPE.isAssignableFrom(obj.getClass())) {
            try {
                final Method method = obj.getClass().getMethod("getOrder");
                AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                    method.setAccessible(true);
                    return null;
                });
                return (Integer) method.invoke(obj);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static final class Helper {
        static final OrderedComparator COMPARATOR = new OrderedComparator();
    }
}
