package cn.maarlakes.common.locks;

import java.lang.annotation.*;

/**
 * 标注在方法上，在方法执行时自动获取分布式锁。
 *
 * <p>此注解需要配合 Spring AOP 使用，由 {@link SyncLockMethodInterceptor} 进行拦截处理。
 * 拦截器会根据注解属性解析锁上下文、获取锁、执行目标方法，最后释放锁。</p>
 *
 * <h3>锁 key 的确定</h3>
 * <p>通过 {@link #value()} 指定锁 key，支持以下格式：</p>
 * <ul>
 *     <li>空字符串（默认）— 使用 {@code 类全限定名.方法名} 作为 key</li>
 *     <li>SpEL 表达式 — 以 {@code #} 开头，如 {@code "#userId"}、{@code "#order.id"}，
 *         由 {@link SpelLockKeyResolver} 解析</li>
 *     <li>普通字符串 — 直接作为 key 使用</li>
 * </ul>
 *
 * <h3>异步模式自动检测</h3>
 * <p>当方法返回类型为 {@link java.util.concurrent.CompletionStage}、{@link java.util.concurrent.Callable}、
 * {@link java.lang.Runnable} 或 {@link java.util.function.Supplier} 时，拦截器自动使用异步锁，
 * 在异步操作完成后释放锁。其他返回类型使用同步锁，方法执行完毕后立即释放。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * // 默认 key（类名.方法名）
 * @SyncLock
 * public void process() { ... }
 *
 * // SpEL 表达式 key
 * @SyncLock("#orderId")
 * public void processOrder(String orderId) { ... }
 *
 * // 公平锁 + 超时等待 + 自动释放
 * @SyncLock(value = "#userId", fair = true, waitTime = 5000, leaseTime = 30000)
 * public void updateUser(String userId) { ... }
 * }</pre>
 *
 * <h3>组合注解</h3>
 * <p>此注解的 {@code @Target} 包含 {@link ElementType#ANNOTATION_TYPE}，
 * 因此可以用于创建组合注解（将多个锁配置组合为一个自定义注解）。</p>
 *
 * @author linjpxc
 * @see SyncLockMethodInterceptor
 * @see SpelLockKeyResolver
 * @see SyncLockContextResolver
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.METHOD})
public @interface SyncLock {

    /**
     * 锁 key，支持 SpEL 表达式。
     *
     * <p>为空时使用默认 key（{@code 类全限定名.方法名}）。
     * 以 {@code #} 开头的表达式通过 SpEL 解析，如 {@code "#userId"}、{@code "#user.id"}。
     * 其他字符串直接作为 key 使用。</p>
     */
    String value() default "";

    /**
     * 是否使用公平锁。
     *
     * <p>公平锁按照线程请求的顺序分配锁，避免线程饥饿，但吞吐量通常低于非公平锁。
     * 默认 {@code false}（非公平锁）。</p>
     */
    boolean fair() default false;

    /**
     * 获取锁的等待时间（毫秒）。
     *
     * <ul>
     *     <li>{@code -1}（默认）— 无限等待，阻塞直到获取锁</li>
     *     <li>{@code 0} — 不等待，获取失败立即抛出 {@link SyncLockTimeoutException}</li>
     *     <li>{@code > 0} — 超时等待，超时后抛出 {@link SyncLockTimeoutException}</li>
     * </ul>
     */
    long waitTime() default -1;

    /**
     * 锁的持有时间（毫秒），主要用于分布式锁的自动释放。
     *
     * <p>到期后锁由底层实现自动释放，防止因持有者崩溃导致的死锁。
     * {@code -1}（默认）表示依赖实现的默认值（如 Redisson 的 watchdog 自动续期机制）。</p>
     */
    long leaseTime() default -1;
}
