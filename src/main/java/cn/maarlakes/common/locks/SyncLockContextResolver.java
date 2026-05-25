package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * 从 {@link SyncLock} 注解中解析 {@link LockContext} 的策略实现。
 *
 * <p>解析过程分两步：</p>
 * <ol>
 *     <li>通过 {@link AnnotatedElementUtils#findMergedAnnotation} 查找方法上的 {@link SyncLock} 注解。
 *         使用 {@code findMergedAnnotation} 而非 {@code getAnnotation} 的原因是前者支持 Spring 的
 *         组合注解机制（包括 {@code @AliasFor} 属性别名）。</li>
 *     <li>调用 {@link SpelLockKeyResolver#resolveKey} 解析注解中的 key 表达式，
 *         结合注解的 fair/waitTime/leaseTime 属性构建完整的 {@link LockContext}。</li>
 * </ol>
 *
 * @author linjpxc
 * @see LockContextResolver
 * @see SyncLock
 * @see SpelLockKeyResolver
 * @see SyncLockMethodInterceptor
 */
public class SyncLockContextResolver implements LockContextResolver {

    private static final Logger log = LoggerFactory.getLogger(SyncLockContextResolver.class);

    /**
     * 从方法调用上下文中解析锁上下文。
     *
     * <p>查找方法上的 {@link SyncLock} 注解，提取其属性，通过 {@link SpelLockKeyResolver}
     * 解析 key 表达式，组装为 {@link LockContext}。</p>
     *
     * @param invocation AOP 方法调用上下文
     * @return 解析后的锁上下文
     * @throws LockException 如果方法上没有 {@link SyncLock} 注解（编程错误）
     */
    @Nonnull
    @Override
    public LockContext resolve(@Nonnull MethodInvocation invocation) {
        final SyncLock syncLock = AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), SyncLock.class);
        if (syncLock == null) {
            log.error("方法 {} 上未找到 @SyncLock 注解，请检查 AOP 切面配置", invocation.getMethod());
            throw new LockException("SyncLock annotation not found on method: " + invocation.getMethod());
        }

        final String key = SpelLockKeyResolver.resolveKey(syncLock.value(), invocation);
        final LockContext context = LockContext.create(key, syncLock.fair(), syncLock.waitTime(), syncLock.leaseTime());
        if (log.isTraceEnabled()) {
            log.trace("从 @SyncLock 注解解析锁上下文：key='{}', fair={}, waitTime={}ms, leaseTime={}ms",
                    key, syncLock.fair(), syncLock.waitTime(), syncLock.leaseTime());
        }
        return context;
    }
}
