package cn.maarlakes.common.task;

import java.lang.annotation.*;

/**
 * 标注在方法上，按任务名串行化执行。
 *
 * <p>支持 SpEL 表达式作为任务名，例如 {@code @SerialTask("#taskId")}。
 * 如果 value 为空，默认使用 {@code 类全限定名.方法名} 作为任务名。</p>
 *
 * @author linjpxc
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface SerialTask {

    /**
     * 任务名，支持 SpEL 表达式。为空时使用默认任务名。
     */
    String value() default "";

    /**
     * 获取锁的等待时间（毫秒），默认 60 秒。
     */
    long waitTime() default 60000;

    /**
     * 锁的持有时间（毫秒），主要用于分布式锁的自动释放。
     * -1 表示依赖实现的默认值。
     */
    long leaseTime() default -1;

    /**
     * 是否公平锁。
     */
    boolean fair() default false;

    /**
     * 获取锁超时时的执行策略。
     */
    Class<? extends SerialTaskExecuteStrategy> strategy() default IgnoredSerialTaskExecuteStrategy.class;
}
