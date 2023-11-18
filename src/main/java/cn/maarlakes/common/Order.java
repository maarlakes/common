package cn.maarlakes.common;

import java.lang.annotation.*;

/**
 * @author linjpxc
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE, ElementType.METHOD, ElementType.FIELD})
public @interface Order {

    int value() default Ordered.LOWEST;
}
