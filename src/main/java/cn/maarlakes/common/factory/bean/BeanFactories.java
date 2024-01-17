package cn.maarlakes.common.factory.bean;

import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.ClassUtils;
import cn.maarlakes.common.utils.Lazy;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author linjpxc
 */
public final class BeanFactories {
    private BeanFactories() {
    }

    private static final SpiServiceLoader<BeanProvider> PROVIDERS = SpiServiceLoader.loadShared(BeanProvider.class, BeanProvider.class.getClassLoader());

    public static boolean contains(@Nonnull Class<?> beanType) {
        return exec(provider -> provider.contains(beanType)).orElse(false);
    }

    public static boolean contains(@Nonnull String beanName) {
        return exec(provider -> provider.contains(beanName)).orElse(false);
    }

    @Nonnull
    public static <T> T getBean(@Nonnull Class<T> beanType) {
        return exec(provider -> provider.getBeanOrNull(beanType)).orElseThrow(() -> new BeanException("Bean of type " + beanType.getName() + " not found."));
    }

    @Nonnull
    public static <T> Optional<T> getBeanOptional(@Nonnull Class<T> beanType) {
        return Optional.ofNullable(getBeanOrNull(beanType));
    }

    @Nonnull
    public static <T> T getBean(@Nonnull Class<T> beanType, @Nonnull Object... args) {
        return exec(provider -> provider.getBean(beanType, args)).orElseThrow(() -> new BeanException("Bean of type " + beanType.getName() + " not found."));
    }

    public static <T> T getBeanOrNull(@Nonnull Class<T> beanType) {
        return exec(provider -> provider.getBeanOrNull(beanType)).orElse(null);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> T getBean(@Nonnull String beanName) {
        return (T) exec(provider -> provider.getBeanOrNull(beanName)).orElseThrow(() -> new BeanException("Bean of name " + beanName + " not found."));
    }

    @Nonnull
    public static <T> Optional<T> getBeanOptional(@Nonnull String beanName) {
        return Optional.ofNullable(getBeanOrNull(beanName));
    }

    @SuppressWarnings("unchecked")
    public static <T> T getBeanOrNull(@Nonnull String beanName) {
        return (T) exec(provider -> provider.getBeanOrNull(beanName)).orElse(null);
    }

    public static <T> T getBeanOrDefault(@Nonnull Class<T> beanType, @Nonnull T defaultValue) {
        final T bean = getBeanOrNull(beanType);
        if (bean == null) {
            return defaultValue;
        }
        return bean;
    }

    public static <T> T getBeanOrDefault(@Nonnull Class<T> beanType, @Nonnull Supplier<T> defaultValue) {
        final T bean = getBeanOrNull(beanType);
        if (bean == null) {
            return defaultValue.get();
        }
        return bean;
    }

    @Nonnull
    public static <T> T getBeanOrNew(@Nonnull Class<T> beanType, @Nonnull Object... args) {
        return getBeanOrNew(beanType, ClassUtils.parameterTypes(args), args);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public static <T> T getBeanOrNew(@Nonnull Class<T> beanType, @Nonnull Class<?>[] argTypes, @Nonnull Object[] args) {
        final T bean = getBeanOrNull(beanType);
        if (bean != null) {
            return bean;
        }
        return (T) newInstance(ClassUtils.getMatchingAccessibleDeclaredConstructor(beanType, argTypes), args);
    }

    @Nonnull
    public static <T> List<T> getBeans(@Nonnull Class<T> beanType) {
        final SpiServiceLoader<BeanProvider> service = PROVIDERS;
        if (service.isEmpty()) {
            return ReflectBeanProvider.getInstance().getBeans(beanType);
        }
        final List<T> list = new ArrayList<>();
        for (BeanProvider provider : service) {
            list.addAll(provider.getBeans(beanType));
        }
        return list;
    }

    @Nonnull
    public static <T> Map<String, T> getBeanMap(@Nonnull Class<T> beanType) {
        final SpiServiceLoader<BeanProvider> service = PROVIDERS;

        if (service.isEmpty()) {
            return ReflectBeanProvider.getInstance().getBeanMap(beanType);
        }
        final Map<String, T> map = new HashMap<>();
        for (BeanProvider provider : service) {
            map.putAll(provider.getBeanMap(beanType));
        }
        return map;
    }

    @Nonnull
    public static <T> Supplier<T> getBeanLazy(@Nonnull Class<T> beanType) {
        return Lazy.of(() -> getBean(beanType));
    }

    @Nonnull
    public static <T> Supplier<T> getBeanLazy(@Nonnull Class<T> beanType, @Nonnull Object... args) {
        return Lazy.of(() -> getBean(beanType, args));
    }

    @Nonnull
    public static <T> Supplier<T> getBeanOrNullLazy(@Nonnull Class<T> beanType) {
        return Lazy.of(() -> getBeanOrNull(beanType));
    }

    @Nonnull
    public static <T> Supplier<T> getBeanLazy(@Nonnull String beanName) {
        return Lazy.of(() -> getBean(beanName));
    }

    @Nonnull
    public static <T> Supplier<T> getBeanOrNullLazy(@Nonnull String beanName) {
        return Lazy.of(() -> getBeanOrNull(beanName));
    }

    public static <T> Supplier<T> getBeanOrDefaultLazy(@Nonnull Class<T> beanType, @Nonnull T defaultValue) {
        return Lazy.of(() -> getBeanOrDefault(beanType, defaultValue));
    }

    public static <T> Supplier<T> getBeanOrDefaultLazy(@Nonnull Class<T> beanType, @Nonnull Supplier<T> defaultValue) {
        return Lazy.of(() -> getBeanOrDefault(beanType, defaultValue));
    }

    @Nonnull
    public static <T> Supplier<T> getBeanOrNewLazy(@Nonnull Class<T> beanType, @Nonnull Object... args) {
        return Lazy.of(() -> getBeanOrNew(beanType, args));
    }

    @Nonnull
    public static <T> Supplier<T> getBeanOrNewLazy(@Nonnull Class<T> beanType, @Nonnull Class<?>[] argTypes, @Nonnull Object[] args) {
        return Lazy.of(() -> getBeanOrNew(beanType, argTypes, args));
    }

    static <T> T newInstance(@Nonnull Constructor<T> constructor, @Nonnull Object[] args) {
        try {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                constructor.setAccessible(true);
                return null;
            });
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new BeanException(e);
        }
    }

    @Nonnull
    private static <T> Optional<T> exec(Function<BeanProvider, T> func) {
        final SpiServiceLoader<BeanProvider> service = PROVIDERS;
        if (service.isEmpty()) {
            return Optional.ofNullable(func.apply(ReflectBeanProvider.getInstance()));
        }
        for (BeanProvider provider : service) {
            final Optional<T> result = Optional.ofNullable(func.apply(provider));
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }
}
