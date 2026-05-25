package cn.maarlakes.common.chain;

import cn.maarlakes.common.tuple.KeyValuePair;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * 链调用器，持有动态代理实例并提供对每次调用结果的访问。
 *
 * <p>通过 {@link #instance()} 获取代理对象并在其上执行方法调用，
 * 随后通过 {@link #result()} 获取每个处理器的执行结果。
 *
 * @param <H> 处理器类型
 * @param <R> 方法返回值类型
 * @author linjpxc
 */
public interface ChainInvoker<H, R> {

    /**
     * 获取动态代理实例。
     *
     * <p>在代理上调用任意接口方法时，会按序分发给所有已注册的处理器。
     *
     * @return 类型为 {@code H} 的代理实例
     */
    @Nonnull
    H instance();

    /**
     * 获取最近一次代理方法调用的结果列表。
     *
     * <p>列表中每个 {@link KeyValuePair} 的 key 为处理器实例，value 为其返回值。
     * 每次在 {@link #instance()} 上调用方法后，此列表会被刷新。
     *
     * @return 处理器与返回值的配对列表
     */
    @Nonnull
    List<KeyValuePair<H, R>> result();
}
