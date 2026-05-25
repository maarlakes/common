package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 忽略执行策略，获取锁超时时直接放弃本次方法调用并返回 {@code null}。
 *
 * <p>这是 {@link SerialTask#strategy()} 的默认策略。当串行任务获取锁超时时，
 * 本策略不做任何处理，仅返回 {@code null}。对于基本类型返回值的方法，
 * 拦截器会将 {@code null} 替换为对应默认值（如 {@code 0}、{@code false}）。</p>
 *
 * <p>采用持有类（Holder）模式实现线程安全的懒加载单例。</p>
 *
 * @author linjpxc
 * @see SerialTaskExecuteStrategy
 * @see SerialTask#strategy()
 */
public final class IgnoredSerialTaskExecuteStrategy implements SerialTaskExecuteStrategy {

    private static final Logger log = LoggerFactory.getLogger(IgnoredSerialTaskExecuteStrategy.class);

    private IgnoredSerialTaskExecuteStrategy() {
    }

    /**
     * 获取单例实例。
     *
     * @return 本策略的唯一实例
     */
    @Nonnull
    public static IgnoredSerialTaskExecuteStrategy getInstance() {
        return Helper.INSTANCE;
    }

    @Override
    public Object execute(String taskName, @Nonnull MethodInvocation invocation) {
        if (log.isDebugEnabled()) {
            log.debug("串行任务 [{}] 获取锁超时，跳过本次调用，方法：{}", taskName, invocation.getMethod());
        }
        return null;
    }

    // 持有类单例模式，利用类加载机制保证线程安全
    private static final class Helper {
        public static final IgnoredSerialTaskExecuteStrategy INSTANCE = new IgnoredSerialTaskExecuteStrategy();
    }
}
