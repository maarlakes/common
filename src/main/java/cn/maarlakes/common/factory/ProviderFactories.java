package cn.maarlakes.common.factory;

import cn.maarlakes.common.AnnotationOrderComparator;
import cn.maarlakes.common.utils.Lazy;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Array;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

/**
 * @author linjpxc
 */
public final class ProviderFactories {
    private ProviderFactories() {
    }

    private final static ConcurrentMap<Object, Object> SINGLETON_PROVIDERS = new ConcurrentHashMap<>();
    private final static ConcurrentMap<Object, Object> SINGLETON_PROVIDER = new ConcurrentHashMap<>();

    public static <T> Supplier<T> getProvider(@Nonnull Class<T> type, @Nonnull Supplier<T> defaultProvider) {
        return Lazy.of(() -> {
            final T[] array = doGetProviders(type, null).get();
            if (array == null || array.length < 1) {
                return defaultProvider.get();
            }
            return array[0];
        });
    }

    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> getSingletonProvider(@Nonnull Class<T> type, @Nonnull Supplier<T> defaultProvider) {
        return (Supplier<T>) SINGLETON_PROVIDER.computeIfAbsent(type, k -> getProvider(type, defaultProvider));
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T[]> getProviders(@Nonnull Class<T> type) {
        return getProviders(type, () -> (T[]) Array.newInstance(type, 0));
    }

    @Nonnull
    public static <T> Supplier<T[]> getProviders(@Nonnull Class<T> type, @Nonnull Supplier<T[]> defaultProviders) {
        return doGetProviders(type, Objects.requireNonNull(defaultProviders));
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T[]> getSingletonProviders(@Nonnull Class<T> type) {
        return getSingletonProviders(type, () -> (T[]) Array.newInstance(type, 0));
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T[]> getSingletonProviders(@Nonnull Class<T> type, @Nonnull Supplier<T[]> defaultProviders) {
        return (Supplier<T[]>) SINGLETON_PROVIDERS.computeIfAbsent(type, k -> getProviders(type, defaultProviders));
    }

    @SuppressWarnings("unchecked")
    private static <T> Supplier<T[]> doGetProviders(@Nonnull Class<T> type, Supplier<T[]> defaultProviders) {
        return Lazy.of(() -> {
            final T[] array = StreamSupport.stream(ServiceLoader.load(type).spliterator(), false)
                    .sorted(AnnotationOrderComparator.getInstance())
                    .toArray(count -> (T[]) Array.newInstance(type, count));
            if (array.length < 1 && defaultProviders != null) {
                return defaultProviders.get();
            }
            return array;
        });
    }
}
