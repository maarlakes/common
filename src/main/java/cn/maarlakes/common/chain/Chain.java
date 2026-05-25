package cn.maarlakes.common.chain;

import jakarta.annotation.Nonnull;

/**
 * 责任链。
 *
 * <p>每次调用 {@link #create()} 都会生成一个独立的 {@link ChainInvoker}，
 * 其中包含按指定策略分发的处理器数组以及对应的动态代理实例。
 *
 * @param <H> 处理器类型，通常是定义了链式调用方法的接口
 * @param <R> 方法返回值类型
 * @author linjpxc
 */
public interface Chain<H, R> {

    /**
     * 创建链调用器。
     *
     * @return 包含代理实例和结果收集能力的调用器
     */
    @Nonnull
    ChainInvoker<H, R> create();
}
