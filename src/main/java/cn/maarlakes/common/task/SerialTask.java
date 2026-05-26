package cn.maarlakes.common.task;

import java.lang.annotation.*;

/**
 * 标注在方法上，使该方法按任务名串行化执行。
 *
 * <p>被标注的方法在执行前会先获取一把与任务名绑定的锁，同一任务名的多次调用将排队执行。
 * 任务名支持 SpEL 表达式，例如 {@code @SerialTask("#taskId")}，可以根据方法参数动态确定锁 key。
 * 如果 {@link #value()} 为空，默认使用 {@code 类全限定名.方法名} 作为任务名。</p>
 *
 * <h3>使用示例</h3>
 * <pre><code>
 * // 静态任务名
 * &#64;SerialTask("order-process")
 * public void processOrder(Order order) { ... }
 *
 * // 动态任务名（SpEL 表达式引用方法参数）
 * &#64;SerialTask("#orderId")
 * public void processOrder(String orderId, Order order) { ... }
 * </code></pre>
 *
 * <h3>锁超时策略</h3>
 * <p>当方法在 {@link #waitTime()} 内未能获取到锁时，会执行 {@link #strategy()} 指定的策略。
 * 默认策略 {@link IgnoredSerialTaskExecuteStrategy} 直接放弃本次调用并返回 {@code null}。</p>
 *
 * @author linjpxc
 * @see SerialTaskExecuteStrategy
 * @see IgnoredSerialTaskExecuteStrategy
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface SerialTask {

    /**
     * 任务名，同时作为锁的 key。支持 SpEL 表达式引用方法参数。
     *
     * <p>为空时使用默认命名规则：{@code 类全限定名.方法名}。</p>
     */
    String value() default "";

    /**
     * 获取锁的最大等待时间（毫秒）。
     *
     * <p>超过此时间仍未获取到锁时，将执行 {@link #strategy()} 指定的降级策略。</p>
     */
    long waitTime() default 60000;

    /**
     * 锁的持有时间（毫秒），主要用于分布式锁的自动释放，防止死锁。
     *
     * <p>设置为 {@code -1} 表示使用锁实现的默认值（例如 Redisson 的看门狗机制默认 30 秒）。</p>
     */
    long leaseTime() default -1;

    /**
     * 是否使用公平锁。
     *
     * <p>公平锁保证按请求顺序（FIFO）获取锁；非公平锁允许插队，吞吐量更高但顺序不可预测。</p>
     */
    boolean fair() default false;

    /**
     * 获取锁超时时的执行策略。
     *
     * <p>默认为 {@link IgnoredSerialTaskExecuteStrategy}，即放弃本次调用。
     * 可指定自定义策略类，通过 {@link cn.maarlakes.common.factory.bean.BeanFactories} 从容器获取实例。</p>
     */
    Class<? extends SerialTaskExecuteStrategy> strategy() default IgnoredSerialTaskExecuteStrategy.class;
}
