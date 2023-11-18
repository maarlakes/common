package cn.maarlakes.common;

import jakarta.annotation.Nonnull;

import java.util.Comparator;

/**
 * @author linjpxc
 */
public class OrderedComparator implements Comparator<Object> {

    protected OrderedComparator() {
    }

    @Nonnull
    public static OrderedComparator getInstance() {
        return Helper.COMPARATOR;
    }

    @Override
    public int compare(Object obj1, Object obj2) {
        if (obj1 == obj2) {
            return 0;
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
        return null;
    }

    private static final class Helper {
        static final OrderedComparator COMPARATOR = new OrderedComparator();
    }
}
