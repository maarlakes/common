package cn.maarlakes.common.spi;

import cn.maarlakes.common.AnnotationOrderComparator;
import cn.maarlakes.common.Ordered;
import cn.maarlakes.common.utils.Lazy;
import jakarta.annotation.Nonnull;

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
import java.util.stream.Stream;

/**
 * 自定义 SPI 服务加载器，从 {@code META-INF/services/} 配置文件中发现和实例化服务提供者。
 *
 * <p>配置文件以服务接口的全限定名为文件名，每行一个实现类的全限定名，支持 {@code #} 注释，同名实现类只加载一次。</p>
 *
 * <p>提供者按 {@code @Order} 注解（或 Spring 的 {@code @Order}）排序，数值越小优先级越高。
 * 可通过 {@link SpiService @SpiService} 注解控制实例的生命周期。</p>
 *
 * <p>两种加载模式：</p>
 * <ul>
 *   <li>{@link #load(Class) load} — 每次调用创建新的 loader 实例，{@code SINGLETON} 生命周期仅在同一 loader 实例内有效</li>
 *   <li>{@link #loadShared(Class) loadShared} — 按 ClassLoader + 服务接口缓存 loader 实例，{@code SINGLETON} 跨调用复用</li>
 * </ul>
 *
 * <pre>{@code
 * // 配置文件: META-INF/services/com.example.MyService
 * com.example.MyServiceImpl
 * com.example.AnotherImpl  # 注释
 *
 * // 使用
 * SpiServiceLoader<MyService> loader = SpiServiceLoader.loadShared(MyService.class);
 * MyService service = loader.first();
 * }</pre>
 *
 * @param <T> 服务接口类型
 * @author linjpxc
 * @see SpiService
 */
public final class SpiServiceLoader<T> implements Iterable<T> {

    private static final String PREFIX = "META-INF/services/";

    private static final ConcurrentMap<ClassLoader, ConcurrentMap<Class<?>, SpiServiceLoader<?>>> SERVICE_LOADER_CACHE = new ConcurrentHashMap<>();

    private final Class<T> service;

    private final ClassLoader loader;
    private final boolean isShared;

    private final Supplier<Collection<Holder>> holders = Lazy.of(this::loadServiceHolder);
    private final ConcurrentMap<Class<?>, T> serviceCache = new ConcurrentHashMap<>();

    private SpiServiceLoader(@Nonnull Class<T> service, ClassLoader loader, boolean isShared) {
        this.service = Objects.requireNonNull(service, "Service interface cannot be null");
        this.loader = getClassLoader(service, loader);
        this.isShared = isShared;
    }

    /**
     * 是否存在可用的服务提供者。
     */
    public boolean isEmpty() {
        return this.holders.get().isEmpty();
    }

    /**
     * 获取优先级最高的服务提供者。
     *
     * @throws SpiServiceException 如果没有可用的提供者
     */
    @Nonnull
    public T first() {
        return this.firstOptional().orElseThrow(() -> new SpiServiceException("No service provider found for " + this.service.getName()));
    }

    /**
     * 获取指定子类型中优先级最高的服务提供者。
     *
     * @param serviceType 目标子类型
     * @throws SpiServiceException 如果没有匹配的提供者
     */
    @Nonnull
    public T first(@Nonnull Class<? extends T> serviceType) {
        return this.firstOptional(serviceType).orElseThrow(() -> new SpiServiceException("No service provider found for " + serviceType.getName()));
    }

    /**
     * 获取优先级最高的服务提供者，无则返回 {@link Optional#empty()}。
     */
    @Nonnull
    public Optional<T> firstOptional() {
        return this.firstOptional(this.service);
    }

    /**
     * 获取指定子类型中优先级最高的服务提供者，无则返回 {@link Optional#empty()}。
     *
     * @param serviceType 目标子类型
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public Optional<T> firstOptional(@Nonnull Class<? extends T> serviceType) {
        return (Optional<T>) this.stream(serviceType).findFirst();
    }

    /**
     * 获取优先级最低的服务提供者。
     *
     * @throws SpiServiceException 如果没有可用的提供者
     */
    @Nonnull
    public T last() {
        return this.lastOptional().orElseThrow(() -> new SpiServiceException("No service provider found for " + this.service.getName()));
    }

    /**
     * 获取优先级最低的服务提供者，无则返回 {@link Optional#empty()}。
     */
    @Nonnull
    public Optional<T> lastOptional() {
        return this.lastOptional(this.service);
    }

    /**
     * 获取指定子类型中优先级最低的服务提供者。
     *
     * @param serviceType 目标子类型
     * @throws SpiServiceException 如果没有匹配的提供者
     */
    @Nonnull
    public T last(@Nonnull Class<? extends T> serviceType) {
        return this.lastOptional(serviceType).orElseThrow(() -> new SpiServiceException("No service provider found for " + serviceType.getName()));
    }

    /**
     * 获取指定子类型中优先级最低的服务提供者，无则返回 {@link Optional#empty()}。
     *
     * @param serviceType 目标子类型
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public Optional<T> lastOptional(@Nonnull Class<? extends T> serviceType) {
        return (Optional<T>) this.stream(serviceType, true).findFirst();
    }

    /**
     * 按优先级顺序（由低到高）返回所有服务提供者的迭代器。
     */
    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return this.stream().iterator();
    }

    /**
     * 按优先级顺序返回所有服务提供者的流。
     */
    @Nonnull
    public Stream<T> stream() {
        return this.stream(this.service);
    }

    /**
     * 按优先级顺序返回指定子类型的服务提供者流。
     *
     * @param serviceType 目标子类型
     */
    @SuppressWarnings("unchecked")
    public <S extends T> Stream<S> stream(@Nonnull Class<S> serviceType) {
        return (Stream<S>) this.stream(serviceType, false);
    }

    /**
     * 创建非共享的服务加载器，使用当前线程的上下文 ClassLoader。
     *
     * <p>每次调用创建新的 loader 实例，{@link SpiService.Lifecycle#SINGLETON SINGLETON} 生命周期
     * 仅在同一 loader 实例内生效。如需跨调用复用单例，请使用 {@link #loadShared(Class)}。</p>
     *
     * @param service 服务接口类型
     */
    @Nonnull
    public static <S> SpiServiceLoader<S> load(@Nonnull Class<S> service) {
        return load(service, Thread.currentThread().getContextClassLoader());
    }

    /**
     * 创建非共享的服务加载器，使用指定的 ClassLoader。
     *
     * @param service 服务接口类型
     * @param loader  用于加载资源的 ClassLoader，为 null 时依次尝试 service 的 ClassLoader 和系统 ClassLoader
     */
    @Nonnull
    public static <S> SpiServiceLoader<S> load(@Nonnull Class<S> service, ClassLoader loader) {
        return new SpiServiceLoader<>(service, loader, false);
    }

    /**
     * 获取共享的服务加载器，使用当前线程的上下文 ClassLoader。
     *
     * <p>按 ClassLoader + 服务接口缓存 loader 实例，{@link SpiService.Lifecycle#SINGLETON SINGLETON}
     * 生命周期跨调用复用。无可用提供者时自动从缓存中移除。</p>
     *
     * @param service 服务接口类型
     */
    public static <S> SpiServiceLoader<S> loadShared(@Nonnull Class<S> service) {
        return loadShared(service, Thread.currentThread().getContextClassLoader());
    }

    /**
     * 获取共享的服务加载器，使用指定的 ClassLoader。
     *
     * @param service 服务接口类型
     * @param loader  用于加载资源的 ClassLoader，为 null 时使用系统 ClassLoader
     */
    @SuppressWarnings("unchecked")
    public static <S> SpiServiceLoader<S> loadShared(@Nonnull Class<S> service, ClassLoader loader) {
        final ClassLoader cl = getClassLoader(service, loader);
        return (SpiServiceLoader<S>) SERVICE_LOADER_CACHE.computeIfAbsent(cl, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(service, k -> new SpiServiceLoader<>(service, cl, true));
    }

    private <S extends T> Stream<? extends S> stream(@Nonnull Class<? extends S> serviceType, boolean reversed) {
        Comparator<Holder> holderComparator = Comparator.comparingInt(h -> {
            Integer order = AnnotationOrderComparator.findOrder(h.serviceType);
            return order != null ? order : Ordered.LOWEST;
        });
        if (reversed) {
            holderComparator = holderComparator.reversed();
        }
        return this.holders.get().stream()
                .filter(item -> serviceType.isAssignableFrom(item.serviceType))
                .sorted(holderComparator)
                .map(this::loadService)
                .map(serviceType::cast);
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

    private Collection<Holder> loadServiceHolder() {
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
                                        throw new SpiServiceException(this.service.getName() + ": Provider " + ln + " not a subtype");
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

            return map.values();
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

    @Nonnull
    private static ClassLoader getClassLoader(@Nonnull Class<?> service, ClassLoader loader) {
        if (loader != null) {
            return loader;
        }
        final ClassLoader classLoader = service.getClassLoader();
        if (classLoader != null) {
            return classLoader;
        }
        return ClassLoader.getSystemClassLoader();
    }

    private static final class Holder {

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
    }
}
