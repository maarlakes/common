package cn.maarlakes.common.spi;

import java.lang.annotation.*;

/**
 * @author linjpxc
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE})
public @interface SpiService {

    int order() default 0;

    Lifecycle lifecycle() default Lifecycle.NEW;

    enum Lifecycle {

        NEW,

        SINGLETON,
    }
}
