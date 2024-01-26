package cn.maarlakes.common.event;

import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author linjpxc
 */
@Inherited
@Documented
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface EventListener {

    int order() default 0;

    boolean async() default false;

    Class<?>[] events() default {};
}
