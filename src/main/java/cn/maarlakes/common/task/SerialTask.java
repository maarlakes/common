package cn.maarlakes.common.task;

import java.lang.annotation.*;

/**
 * @author linjpxc
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface SerialTask {

    String value() default "";

    int timeout() default 60000;

    boolean fair() default false;

    Class<? extends SerialTaskExecuteStrategy> strategy() default IgnoredSerialTaskExecuteStrategy.class;
}
