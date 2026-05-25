package cn.maarlakes.common.chain;

import jakarta.annotation.Nonnull;

/**
 * 链调用器工厂，负责将处理器数组封装为 {@link ChainInvoker}。
 *
 * <p>实现类决定处理器的执行顺序（正序/逆序）以及结果的收集策略。
 *
 * @author linjpxc
 */
public interface ChainInvocationFactory {

    /**
     * 创建链调用器。
     *
     * @param type     处理器接口类型
     * @param handlers 已排序的处理器数组
     * @param <H>      处理器类型
     * @param <R>      方法返回值类型
     * @return 可执行链式调用并收集结果的调用器
     */
    @Nonnull
    <H, R> ChainInvoker<H, R> create(@Nonnull Class<H> type, @Nonnull H[] handlers);
}
