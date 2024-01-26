package cn.maarlakes.common.spi;

import cn.maarlakes.common.utils.Lazy;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author linjpxc
 */
public final class SpiServiceLoader<T> implements Iterable<T> {

    private static final String PREFIX = "META-INF/services/";

    private static final ConcurrentMap<ClassLoader, ConcurrentMap<Class<?>, SpiServiceLoader<?>>> SERVICE_LOADER_CACHE = new ConcurrentHashMap<>();

    private final Class<T> service;

    private final ClassLoader loader;
    private final boolean isShared;

    private final Supplier<List<Holder>> holders = Lazy.of(this::loadServiceHolder);
    private final ConcurrentMap<Class<?>, T> serviceCache = new ConcurrentHashMap<>();

    private SpiServiceLoader(@Nonnull Class<T> service, ClassLoader loader, boolean isShared) {
        this.service = Objects.requireNonNull(service, "Service interface cannot be null");
        this.loader = (loader == null) ? service.getClassLoader() : loader;
        this.isShared = isShared;
    }

    public boolean isEmpty() {
        return this.holders.get().isEmpty();
    }

    @Nonnull
    public T first() {
        return this.firstOptional().orElseThrow(() -> new SpiServiceException("No service provider found for " + this.service.getName()));
    }

    @Nonnull
    public T first(@Nonnull Class<? extends T> serviceType) {
        return this.firstOptional(serviceType).orElseThrow(() -> new SpiServiceException("No service provider found for " + serviceType.getName()));
    }

    @Nonnull
    public Optional<T> firstOptional() {
        return this.firstOptional(this.service);
    }

    @Nonnull
    public Optional<T> firstOptional(@Nonnull Class<? extends T> serviceType) {
        return this.holders.get().stream().filter(item -> serviceType.isAssignableFrom(item.serviceType)).map(this::loadService).findFirst();
    }

    @Nonnull
    public T last() {
        return this.lastOptional().orElseThrow(() -> new SpiServiceException("No service provider found for " + this.service.getName()));
    }

    @Nonnull
    public Optional<T> lastOptional() {
        return this.lastOptional(this.service);
    }

    @Nonnull
    public T last(@Nonnull Class<? extends T> serviceType) {
        return this.lastOptional(serviceType).orElseThrow(() -> new SpiServiceException("No service provider found for " + serviceType.getName()));
    }

    @Nonnull
    public Optional<T> lastOptional(@Nonnull Class<? extends T> serviceType) {
        return this.holders.get().stream()
                .filter(item -> serviceType.isAssignableFrom(item.serviceType))
                .sorted((left, right) -> Holder.compare(right, left))
                .map(this::loadService)
                .findFirst();
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return this.holders.get().stream().map(this::loadService).iterator();
    }

    @Nonnull
    public Stream<T> stream() {
        return this.holders.get().stream().map(this::loadService);
    }

    @Nonnull
    public static <S> SpiServiceLoader<S> load(@Nonnull Class<S> service) {
        return load(service, Thread.currentThread().getContextClassLoader());
    }

    @Nonnull
    public static <S> SpiServiceLoader<S> load(@Nonnull Class<S> service, ClassLoader loader) {
        return new SpiServiceLoader<>(service, loader, false);
    }

    public static <S> SpiServiceLoader<S> loadShared(@Nonnull Class<S> service) {
        return loadShared(service, Thread.currentThread().getContextClassLoader());
    }

    @SuppressWarnings("unchecked")
    public static <S> SpiServiceLoader<S> loadShared(@Nonnull Class<S> service, ClassLoader loader) {
        final ClassLoader cl = loader == null ? ClassLoader.getSystemClassLoader() : loader;
        return (SpiServiceLoader<S>) SERVICE_LOADER_CACHE.computeIfAbsent(cl, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(service, k -> new SpiServiceLoader<>(service, cl, true));
    }

    private T loadService(@Nonnull Holder holder) {
        if (holder.spiService != null && holder.spiService.lifecycle() == SpiService.Lifecycle.SINGLETON) {
            return this.serviceCache.computeIfAbsent(holder.serviceType, k -> this.createService(holder));
        }
        return this.createService(holder);
    }

    private T createService(@Nonnull Holder holder) {
        try {
            return this.service.cast(holder.serviceType.getConstructor().newInstance());
        } catch (Exception e) {
            throw new SpiServiceException(e.getMessage(), e);
        }
    }


    private List<Holder> loadServiceHolder() {
        final Enumeration<URL> configs = this.loadConfigs();
        try {
            final Map<String, Holder> map = new HashMap<>();
            while (configs.hasMoreElements()) {
                final URL url = configs.nextElement();
                try (InputStream in = url.openStream()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                        String ln;
                        int lineNumber = 0;
                        while ((ln = reader.readLine()) != null) {
                            lineNumber++;
                            final int ci = ln.indexOf('#');
                            if (ci >= 0) {
                                ln = ln.substring(0, ci);
                            }
                            ln = ln.trim();
                            if (!ln.isEmpty()) {
                                this.check(url, ln, lineNumber);
                                if (!map.containsKey(ln)) {
                                    final Class<?> type = Class.forName(ln, false, loader);
                                    if (!this.service.isAssignableFrom(type)) {
                                        throw new SpiServiceException(this.service.getName() + ": Provider" + ln + " not a subtype");
                                    }
                                    map.put(ln, new Holder(type, type.getAnnotation(SpiService.class)));
                                }
                            }
                        }
                    }
                }
            }
            if (map.isEmpty() && this.isShared) {
                // 移除
                this.remove();
            }

            return map.values().stream().sorted().collect(Collectors.toList());
        } catch (IOException | ClassNotFoundException e) {
            if (this.isShared) {
                this.remove();
            }
            throw new SpiServiceException(e);
        }
    }

    private void remove() {
        final ConcurrentMap<Class<?>, SpiServiceLoader<?>> map = SERVICE_LOADER_CACHE.get(this.loader);
        if (map != null) {
            map.remove(this.service);
        }
    }

    private Enumeration<URL> loadConfigs() {
        final String fullName = PREFIX + service.getName();
        try {
            return this.loader.getResources(fullName);
        } catch (IOException e) {
            throw new SpiServiceException(service.getName() + ": Error locating configuration files", e);
        }
    }

    private void check(@Nonnull URL url, @Nonnull String className, int lineNumber) {
        if ((className.indexOf(' ') >= 0) || (className.indexOf('\t') >= 0)) {
            throw new SpiServiceException(this.service.getName() + ": " + className + ":" + lineNumber + ":Illegal configuration-file syntax");
        }
        int cp = className.codePointAt(0);
        if (!Character.isJavaIdentifierStart(cp)) {
            throw new SpiServiceException(this.service.getName() + ": " + url + ":" + lineNumber + ":Illegal provider-class name: " + className);
        }
        final int length = className.length();
        for (int i = Character.charCount(cp); i < length; i += Character.charCount(cp)) {
            cp = className.codePointAt(i);
            if (!Character.isJavaIdentifierPart(cp) && (cp != '.')) {
                throw new SpiServiceException(this.service.getName() + ": " + url + ":" + lineNumber + ":Illegal provider-class name: " + className);
            }
        }
    }

    private static final class Holder implements Comparable<Holder> {

        private final Class<?> serviceType;

        private final SpiService spiService;

        private Holder(@Nonnull Class<?> serviceType, SpiService spiService) {
            this.serviceType = serviceType;
            this.spiService = spiService;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Holder) {
                return this.serviceType == ((Holder) obj).serviceType;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceType);
        }

        @Override
        public int compareTo(@Nullable Holder o) {
            if (o == null) {
                return 1;
            }
            return Integer.compare(this.spiService == null ? Integer.MAX_VALUE : this.spiService.order(), o.spiService == null ? Integer.MAX_VALUE : o.spiService.order());
        }

        public static int compare(Holder left, Holder right) {
            if (left == right) {
                return 0;
            }
            if (left == null) {
                return -1;
            }
            return left.compareTo(right);
        }
    }
}
