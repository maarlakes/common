package cn.maarlakes.common;

import cn.maarlakes.common.utils.ClassUtils;
import cn.maarlakes.common.utils.MethodUtils;
import jakarta.annotation.Nonnull;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * @author linjpxc
 */
public class AnnotationOrderComparator extends OrderedComparator {

    private static final Class<? extends Annotation> SPRING_ORDER_ANNOTATION_TYPE;

    static {
        SPRING_ORDER_ANNOTATION_TYPE = ClassUtils.loadClass("org.springframework.core.annotation.Order");
    }

    protected AnnotationOrderComparator() {
    }

    @Nonnull
    public static AnnotationOrderComparator getInstance() {
        return Helper.INSTANCE;
    }

    @Override
    protected Integer findOrder(@Nonnull Object obj) {
        final Integer order = super.findOrder(obj);
        if (order != null) {
            return order;
        }
        return findOrder(obj.getClass());
    }

    public static Integer findOrder(@Nonnull AnnotatedElement element) {
        final Order order = element.getAnnotation(Order.class);
        if (order != null) {
            return order.value();
        }
        if (SPRING_ORDER_ANNOTATION_TYPE == null) {
            return null;
        }
        final Annotation annotation = element.getAnnotation(SPRING_ORDER_ANNOTATION_TYPE);
        if (annotation == null) {
            return null;
        }
        try {
            final Method method = annotation.annotationType().getMethod("value");
            return MethodUtils.invoke(method, annotation);
        } catch (Exception ignored) {
            return null;
        }
    }


    private static final class Helper {
        private static final AnnotationOrderComparator INSTANCE = new AnnotationOrderComparator();
    }
}
