package cn.maarlakes.common.locks;

import java.lang.annotation.*;

/**
 * @author linjpxc
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
public @interface SyncLock {
    String value() default "";

    boolean fair() default false;

    long timeout() default 0;

    boolean supportAsync() default false;
}
