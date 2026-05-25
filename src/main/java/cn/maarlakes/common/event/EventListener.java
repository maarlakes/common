package cn.maarlakes.common.event;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 标注事件监听方法。
 *
 * <p>被此注解标注的方法会被 {@link EventListenerHandler} 自动发现并注册为事件监听器。
 * 方法必须至少有一个参数，该参数的类型决定了方法能接收的事件类型。
 *
 * <p><b>事件类型匹配规则：</b>
 * <ul>
 *   <li>若未指定 {@link #events()}，则按方法参数类型进行 {@code isAssignableFrom} 匹配</li>
 *   <li>若指定了 {@link #events()}，则只有事件类型在列表中且方法参数类型也兼容时才匹配</li>
 * </ul>
 *
 * @author linjpxc
 * @see EventDispatch
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface EventListener {

    /**
     * 指定监听器订阅的事件类型列表。
     *
     * <p>为空数组（默认）时，根据方法参数类型自动推断支持的监听事件类型；
     * 非空时，只有事件类型在此列表中才会触发该监听方法。
     *
     * @return 订阅的事件类型数组，默认为空
     */
    Class<?>[] events() default {};
}
