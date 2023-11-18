package cn.maarlakes.common.factory.bean;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * @author linjpxc
 */
public interface BeanProvider {

    <T> boolean contains(@Nonnull Class<T> beanType);

    boolean contains(@Nonnull String beanName);

    @Nonnull
    <T> T getBean(@Nonnull Class<T> beanType);

    <T> T getBeanOrNull(@Nonnull Class<T> beanType);

    @Nonnull
    <T> T getBean(@Nonnull String beanName);

    <T> T getBeanOrNull(@Nonnull String beanName);

    @Nonnull
    <T> T getBean(@Nonnull Class<T> beanType, @Nonnull Object... args);

    @Nonnull
    <T> List<T> getBeans(@Nonnull Class<T> beanType);

    @Nonnull
    <T> Map<String, T> getBeanMap(@Nonnull Class<T> beanType);
}
