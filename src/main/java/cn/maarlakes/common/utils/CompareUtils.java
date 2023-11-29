package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public final class CompareUtils {
    private CompareUtils() {
    }

    @Nonnull
    public static <T extends Comparable<? super T>> T min(@Nonnull T first, @Nonnull T... others) {
        for (T other : others) {
            if (first.compareTo(other) > 0) {
                first = other;
            }
        }
        return first;
    }

    @Nonnull
    public static <T extends Comparable<? super T>> T max(@Nonnull T first, @Nonnull T... others) {
        for (T other : others) {
            if (first.compareTo(other) < 0) {
                first = other;
            }
        }
        return first;
    }
}
