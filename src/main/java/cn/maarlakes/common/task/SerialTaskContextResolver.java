package cn.maarlakes.common.task;

import cn.maarlakes.common.locks.LockContext;
import cn.maarlakes.common.locks.LockContextResolver;
import cn.maarlakes.common.locks.LockException;
import cn.maarlakes.common.locks.SpelLockKeyResolver;
import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * 从 {@link SerialTask} 注解中解析 {@link LockContext}。
 *
 * <p>解析流程：</p>
 * <ol>
 *     <li>查找方法上合并后的 {@code @SerialTask} 注解（支持组合注解）</li>
 *     <li>通过 {@link SpelLockKeyResolver} 解析 SpEL 表达式，得到锁 key</li>
 *     <li>用锁 key 和注解属性（fair、waitTime、leaseTime）创建 {@link LockContext}</li>
 * </ol>
 *
 * @author linjpxc
 * @see SerialTask
 * @see SpelLockKeyResolver
 */
public class SerialTaskContextResolver implements LockContextResolver {

    private static final Logger log = LoggerFactory.getLogger(SerialTaskContextResolver.class);

    @Nonnull
    @Override
    public LockContext resolve(@Nonnull MethodInvocation invocation) {
        final SerialTask serialTask = AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), SerialTask.class);
        if (serialTask == null) {
            if (log.isDebugEnabled()) {
                log.debug("方法 [{}] 上未找到 @SerialTask 注解", invocation.getMethod());
            }
            throw new LockException("SerialTask annotation not found on method: " + invocation.getMethod());
        }

        final String key = SpelLockKeyResolver.resolveKey(serialTask.value(), invocation);
        if (log.isTraceEnabled()) {
            log.trace("串行任务锁上下文解析完成，锁 key：{}，公平锁：{}，等待时间：{}ms，持有时间：{}ms",
                    key, serialTask.fair(), serialTask.waitTime(), serialTask.leaseTime());
        }
        return LockContext.create(key, serialTask.fair(), serialTask.waitTime(), serialTask.leaseTime());
    }
}
