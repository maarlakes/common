package cn.maarlakes.common.factory.bean;

import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.ClassUtils;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public final class SpiBeanProvider implements BeanProvider {

    private final boolean isShared;

    public SpiBeanProvider(boolean isShared) {
        this.isShared = isShared;
    }

    @Override
    public <T> boolean contains(@Nonnull Class<T> beanType) {
        return this.load(beanType).stream().anyMatch(bean -> true);
    }

    @Override
    public boolean contains(@Nonnull String beanName) {
        final Class<Object> type = ClassUtils.loadClass(beanName);
        if (type == null) {
            return false;
        }
        return contains(type);
    }

    @Nonnull
    @Override
    public <T> T getBean(@Nonnull Class<T> beanType) {
        return this.load(beanType).first();
    }

    @Override
    public <T> T getBeanOrNull(@Nonnull Class<T> beanType) {
        return this.load(beanType).firstOptional().orElse(null);
    }

    @Nonnull
    @Override
    public <T> T getBean(@Nonnull String beanName) {
        final Class<T> type = ClassUtils.loadClass(beanName);
        if (type == null) {
            throw new BeanException("Not found bean [" + beanName + "]");
        }
        return this.getBean(type);
    }

    @Override
    public <T> T getBeanOrNull(@Nonnull String beanName) {
        final Class<T> type = ClassUtils.loadClass(beanName);
        if (type == null) {
            return null;
        }
        return this.load(type).firstOptional().orElse(null);
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getBean(@Nonnull Class<T> beanType, @Nonnull Object... args) {
        try {
            return (T) BeanFactories.newInstance(ClassUtils.getMatchingAccessibleDeclaredConstructor(beanType, ClassUtils.parameterTypes(args)), args);
        } catch (Exception e) {
            throw new BeanException(e.getMessage(), e);
        }
    }

    @Nonnull
    @Override
    public <T> List<T> getBeans(@Nonnull Class<T> beanType) {
        return this.load(beanType).stream().collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public <T> Map<String, T> getBeanMap(@Nonnull Class<T> beanType) {
        return this.getBeans(beanType).stream().collect(Collectors.toMap(item -> item.getClass().getName(), Function.identity()));
    }

    private <T> SpiServiceLoader<T> load(@Nonnull Class<T> type) {
        if (this.isShared) {
            return SpiServiceLoader.loadShared(type, type.getClassLoader());
        }
        return SpiServiceLoader.load(type, type.getClassLoader());
    }
}
