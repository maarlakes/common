package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.lang.annotation.Annotation;

/**
 * @author linjpxc
 */
public interface EventInvoker {

    <A extends Annotation> A getAnnotation(@Nonnull Class<A> annotationType);

    boolean supportedEvent(@Nonnull Class<?> eventType);

    <E> void invoke(@Nonnull E event);
}
