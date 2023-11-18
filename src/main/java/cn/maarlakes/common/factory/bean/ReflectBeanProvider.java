package cn.maarlakes.common.factory.bean;

import cn.maarlakes.common.utils.ClassUtils;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author linjpxc
 */
public final class ReflectBeanProvider implements BeanProvider {

    private ReflectBeanProvider() {
    }

    public static ReflectBeanProvider getInstance() {
        return Helper.INSTANCE;
    }

    @Override
    public <T> boolean contains(@Nonnull Class<T> beanType) {
        return Arrays.stream(beanType.getDeclaredConstructors()).anyMatch(item -> item.getParameterTypes().length == 0);
    }

    @Override
    public boolean contains(@Nonnull String beanName) {
        return false;
    }

    @Nonnull
    @Override
    public <T> T getBean(@Nonnull Class<T> beanType) {
        try {
            final Constructor<T> constructor = beanType.getDeclaredConstructor();
            return BeanFactories.newInstance(constructor, new Object[0]);
        } catch (Exception e) {
            throw new BeanException(e.getMessage(), e);
        }
    }

    @Override
    public <T> T getBeanOrNull(@Nonnull Class<T> beanType) {
        if (this.contains(beanType)) {
            return this.getBean(beanType);
        }
        return null;
    }

    @Nonnull
    @Override
    public <T> T getBean(@Nonnull String beanName) {
        throw new BeanException("Not found bean " + beanName);
    }

    @Override
    public <T> T getBeanOrNull(@Nonnull String beanName) {
        return null;
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
        return Collections.emptyList();
    }

    @Nonnull
    @Override
    public <T> Map<String, T> getBeanMap(@Nonnull Class<T> beanType) {
        return Collections.emptyMap();
    }

    private static final class Helper {
        private static final ReflectBeanProvider INSTANCE = new ReflectBeanProvider();
    }
}
