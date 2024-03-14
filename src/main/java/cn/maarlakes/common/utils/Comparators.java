package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.stream.StreamSupport;

/**
 * @author linjpxc
 */
public final class Comparators {
    private Comparators() {
    }

    @SafeVarargs
    public static <T extends Comparable<? super T>> T min(@Nonnull T... array) {
        return minOptional(array).orElse(null);
    }

    @Nonnull
    @SafeVarargs
    public static <T extends Comparable<? super T>> Optional<T> minOptional(@Nonnull T... array) {
        return minOptional(array, Comparator.naturalOrder());
    }

    @SafeVarargs
    public static <T> T min(@Nonnull Comparator<? super T> comparator, @Nonnull T... array) {
        return min(array, comparator);
    }

    @Nonnull
    @SafeVarargs
    public static <T> Optional<T> minOptional(@Nonnull Comparator<? super T> comparator, @Nonnull T... array) {
        return minOptional(array, comparator);
    }

    public static <T> T min(@Nonnull T[] array, @Nonnull Comparator<? super T> comparator) {
        return minOptional(array, comparator).orElse(null);
    }

    @Nonnull
    public static <T> Optional<T> minOptional(@Nonnull T[] array, @Nonnull Comparator<? super T> comparator) {
        return Arrays.stream(array).min(comparator);
    }

    @SafeVarargs
    public static <T extends Comparable<? super T>> T max(@Nonnull T... array) {
        return maxOptional(array, Comparator.naturalOrder()).orElse(null);
    }

    @Nonnull
    @SafeVarargs
    public static <T extends Comparable<? super T>> Optional<T> maxOptional(@Nonnull T... array) {
        return maxOptional(array, Comparator.naturalOrder());
    }

    @SafeVarargs
    public static <T> T max(@Nonnull Comparator<? super T> comparator, @Nonnull T... array) {
        return maxOptional(array, comparator).orElse(null);
    }

    @Nonnull
    @SafeVarargs
    public static <T> Optional<T> maxOptional(@Nonnull Comparator<? super T> comparator, @Nonnull T... array) {
        return maxOptional(array, comparator);
    }

    public static <T> T max(@Nonnull T[] array, @Nonnull Comparator<? super T> comparator) {
        return maxOptional(array, comparator).orElse(null);
    }

    @Nonnull
    public static <T> Optional<T> maxOptional(@Nonnull T[] array, @Nonnull Comparator<? super T> comparator) {
        return Arrays.stream(array).max(comparator);
    }

    public static <T> T min(Iterable<T> list, @Nonnull Comparator<? super T> comparator) {
        return minOptional(list, comparator).orElse(null);
    }

    @Nonnull
    public static <T> Optional<T> minOptional(Iterable<T> list, @Nonnull Comparator<? super T> comparator) {
        if (list instanceof Collection) {
            return ((Collection<T>) list).stream().min(comparator);
        }
        return StreamSupport.stream(list.spliterator(), false).min(comparator);
    }

    public static <T> T max(@Nonnull Iterable<T> list, @Nonnull Comparator<? super T> comparator) {
        return maxOptional(list, comparator).orElse(null);
    }

    @Nonnull
    public static <T> Optional<T> maxOptional(Iterable<T> list, Comparator<? super T> comparator) {
        if (list instanceof Collection) {
            return ((Collection<T>) list).stream().max(comparator);
        }
        return StreamSupport.stream(list.spliterator(), false).max(comparator);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends Comparable<? super T>> T min(@Nonnull Iterable<T> list) {
        return (T) minOptional((Iterable) list).orElse(null);
    }

    @Nonnull
    public static <T extends Comparable<? super T>> Optional<T> minOptional(@Nonnull Iterable<T> list) {
        return minOptional(list, Comparator.naturalOrder());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends Comparable<? super T>> T max(@Nonnull Iterable<T> list) {
        return (T) maxOptional((Iterable) list).orElse(null);
    }

    @Nonnull
    public static <T extends Comparable<? super T>> Optional<T> maxOptional(@Nonnull Iterable<T> list) {
        return maxOptional(list, Comparator.naturalOrder());
    }

    public static <T extends Comparable<? super T>> T min(@Nonnull Iterator<T> iterator) {
        return minOptional(iterator).orElse(null);
    }

    @Nonnull
    public static <T extends Comparable<? super T>> Optional<T> minOptional(@Nonnull Iterator<T> iterator) {
        return minOptional(iterator, Comparator.naturalOrder());
    }

    public static <T> T min(@Nonnull Iterator<T> iterator, Comparator<? super T> comparator) {
        return minOptional(iterator, comparator).orElse(null);
    }

    @Nonnull
    public static <T> Optional<T> minOptional(@Nonnull Iterator<T> iterator, @Nonnull Comparator<? super T> comparator) {
        if (!iterator.hasNext()) {
            return Optional.empty();
        }

        T min = iterator.next();
        while (iterator.hasNext()) {
            final T next = iterator.next();
            if (comparator.compare(min, next) > 0) {
                min = next;
            }
        }
        return Optional.ofNullable(min);
    }

    public static <T extends Comparable<? super T>> T max(@Nonnull Iterator<T> iterator) {
        return maxOptional(iterator).orElse(null);
    }

    @Nonnull
    public static <T extends Comparable<? super T>> Optional<T> maxOptional(@Nonnull Iterator<T> iterator) {
        return maxOptional(iterator, Comparator.naturalOrder());
    }

    public static <T> T max(@Nonnull Iterator<T> iterator, @Nonnull Comparator<? super T> comparator) {
        return maxOptional(iterator, comparator).orElse(null);
    }

    @Nonnull
    public static <T> Optional<T> maxOptional(@Nonnull Iterator<T> iterator, @Nonnull Comparator<? super T> comparator) {
        if (!iterator.hasNext()) {
            return Optional.empty();
        }
        T max = iterator.next();
        while (iterator.hasNext()) {
            final T next = iterator.next();
            if (comparator.compare(max, next) < 0) {
                max = next;
            }
        }

        return Optional.ofNullable(max);
    }
}
