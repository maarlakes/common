package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.lang.annotation.Annotation;

/**
 * 事件调用器，封装了一个监听器方法及其目标对象，负责判断事件类型是否匹配并执行调用。
 *
 * <p>每个 {@code EventInvoker} 实例对应一个被 {@link EventListener} 标注的方法，
 * 由 {@link EventListenerHandler} 在扫描监听器时创建。
 *
 * @author linjpxc
 * @see EventListenerHandler#getInvokers(Object)
 */
public interface EventInvoker {

    /**
     * 从监听器方法或其声明类上获取指定类型的注解。
     *
     * <p>优先从方法上查找，未找到时从声明该方法的类上查找。
     *
     * @param <A>            注解类型
     * @param annotationType 要查找的注解类型
     * @return 找到的注解实例，均未找到时返回 null
     */
    <A extends Annotation> A getAnnotation(@Nonnull Class<A> annotationType);

    /**
     * 判断本调用器是否支持处理指定类型的事件。
     *
     * @param eventType 待检查的事件类型
     * @return 如果监听器方法能接受该事件类型则返回 {@code true}
     */
    boolean supportedEvent(@Nonnull Class<?> eventType);

    /**
     * 使用给定的事件对象调用监听器方法。
     *
     * @param <E>   事件类型
     * @param event 要传递给监听器方法的事件对象
     * @throws EventException 如果监听器方法执行过程中发生异常
     */
    <E> void invoke(@Nonnull E event);
}
