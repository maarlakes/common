package cn.maarlakes.common.chain;

import jakarta.annotation.Nonnull;

import java.lang.reflect.InvocationHandler;

/**
 * {@link InvocationHandler} 工厂，根据处理器类型和实例数组创建对应的调用处理器。
 *
 * <p>不同的工厂实现提供不同的结果收集策略（首个非空、最后一个、忽略返回值等）。
 *
 * @author linjpxc
 */
public interface InvocationHandlerFactory {

    /**
     * 创建调用处理器。
     *
     * @param type     处理器接口类型
     * @param handlers 已排序的处理器数组
     * @param <H>      处理器类型
     * @return 用于 {@link java.lang.reflect.Proxy} 的 {@link InvocationHandler}
     */
    @Nonnull
    <H> InvocationHandler create(@Nonnull Class<H> type, @Nonnull H[] handlers);
}
