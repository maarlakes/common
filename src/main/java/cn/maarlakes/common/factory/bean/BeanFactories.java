package cn.maarlakes.common.factory.bean;

import cn.maarlakes.common.factory.ProviderFactories;
import cn.maarlakes.common.utils.ClassUtils;
import cn.maarlakes.common.utils.Lazy;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Constructor;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author linjpxc
 */
public final class BeanFactories {
    private BeanFactories() {
    }

    private static final Supplier<BeanProvider[]> PROVIDER = ProviderFactories.getProviders(BeanProvider.class, () -> new BeanProvider[]{ReflectBeanProvider.getInstance()});

    public static boolean contains(@Nonnull Class<?> beanType) {
        for (BeanProvider provider : PROVIDER.get()) {
            if (provider.contains(beanType)) {
                return true;
            }
        }
        return false;
    }

    public static boolean contains(@Nonnull String beanName) {
        for (BeanProvider provider : PROVIDER.get()) {
            if (provider.contains(beanName)) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    public static <T> T getBean(@Nonnull Class<T> beanType) {
        for (BeanProvider provider : PROVIDER.get()) {
            if (provider.contains(beanType)) {
                return provider.getBean(beanType);
            }
        }
        throw new RuntimeException("Bean of type " + beanType.getName() + " not found.");
    }

    @Nonnull
    public static <T> T getBean(@Nonnull Class<T> beanType, @Nonnull Object... args) {
        for (BeanProvider provider : PROVIDER.get()) {
            if (provider.contains(beanType)) {
                return provider.getBean(beanType, args);
            }
        }
        throw new RuntimeException("Bean of type " + beanType.getName() + " not found.");
    }

    public static <T> T getBeanOrNull(@Nonnull Class<T> beanType) {
        for (BeanProvider provider : PROVIDER.get()) {
            if (provider.contains(beanType)) {
                return provider.getBeanOrNull(beanType);
            }
        }
        return null;
    }

    public static <T> T getBean(@Nonnull String beanName) {
        for (BeanProvider provider : PROVIDER.get()) {
            if (provider.contains(beanName)) {
                return provider.getBean(beanName);
            }
        }
        throw new RuntimeException("Bean of name " + beanName + " not found.");
    }

    public static <T> T getBeanOrNull(@Nonnull String beanName) {
        for (BeanProvider provider : PROVIDER.get()) {
            if (provider.contains(beanName)) {
                return provider.getBeanOrNull(beanName);
            }
        }
        return null;
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
        final List<T> list = new ArrayList<>();
        for (BeanProvider provider : PROVIDER.get()) {
            list.addAll(provider.getBeans(beanType));
        }
        return list;
    }

    @Nonnull
    public static <T> Map<String, T> getBeanMap(@Nonnull Class<T> beanType) {
        final Map<String, T> map = new HashMap<>();
        for (BeanProvider provider : PROVIDER.get()) {
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
}
