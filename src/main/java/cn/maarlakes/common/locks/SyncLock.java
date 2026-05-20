package cn.maarlakes.common.locks;

import java.lang.annotation.*;

/**
 * 标注在方法上，在执行时自动获取分布式锁。
 *
 * <p>支持 SpEL 表达式作为锁 key，例如 {@code @SyncLock("#userId")}。
 * 如果 value 为空，默认使用 {@code 类全限定名.方法名} 作为 key。</p>
 *
 * @author linjpxc
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
public @interface SyncLock {

    /**
     * 锁 key，支持 SpEL 表达式。为空时使用默认 key。
     */
    String value() default "";

    /**
     * 是否公平锁。
     */
    boolean fair() default false;

    /**
     * 获取锁的等待时间（毫秒）。
     * -1 表示无限等待（默认），大于 0 表示超时等待。
     */
    long waitTime() default -1;

    /**
     * 锁的持有时间（毫秒），主要用于分布式锁的自动释放。
     * -1 表示依赖实现的默认值。
     */
    long leaseTime() default -1;
}
