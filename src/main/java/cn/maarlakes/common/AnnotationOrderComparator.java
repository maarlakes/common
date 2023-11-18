package cn.maarlakes.common;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public class AnnotationOrderComparator extends OrderedComparator {

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
        final Order orderAnnotation = obj.getClass().getAnnotation(Order.class);
        if (orderAnnotation != null) {
            return orderAnnotation.value();
        }
        return null;
    }

    private static final class Helper {
        private static final AnnotationOrderComparator INSTANCE = new AnnotationOrderComparator();
    }
}
