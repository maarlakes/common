package cn.maarlakes.common.event;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 标注事件监听方法的分发策略。
 *
 * <p>可标注在监听方法、类或作为元注解使用。当标注在方法上时，控制该方法对应的事件分发方式；
 * 当标注在类上时，作为该类所有监听方法的默认分发策略。
 *
 * @author linjpxc
 * @see EventListener
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
public @interface EventDispatch {

    /**
     * 是否异步分发事件。
     *
     * <p>为 {@code true} 时，{@link EventDispatcher#dispatchAsync(EventInvoker, Object)}
     * 将在线程池中执行该监听方法，不阻塞事件发布线程。
     * <p>为 {@code false}（默认）时，{@link EventDispatcher#dispatch(EventInvoker, Object)}
     * 在当前线程中同步执行。
     *
     * @return 是否异步执行，默认 {@code false}
     */
    boolean async() default false;
}
