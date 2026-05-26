package cn.maarlakes.common.chain;

import jakarta.annotation.Nonnull;

/**
 * 责任链工厂，负责发现处理器并构建可执行的链式调用。
 *
 * <p>工厂首先通过 {@code createHandlers} 发现指定类型的所有处理器实例，
 * 然后根据不同的结果收集策略创建动态代理或链调用器。
 *
 * <p>提供了三种便捷方法来创建不同结果策略的代理：
 * <ul>
 *   <li>{@link #createFirstResultChain} - 返回第一个非空结果</li>
 *   <li>{@link #createLastResultChain} - 返回最后一个结果</li>
 *   <li>{@link #createNoneResultChain} - 忽略所有结果（返回 null）</li>
 * </ul>
 *
 * @author linjpxc
 */
public interface ChainFactory {

    /**
     * 使用自定义调用器工厂创建链，可获取每个处理器的独立执行结果。
     *
     * @param type    处理器接口类型
     * @param factory 调用器工厂，决定执行顺序和结果收集方式
     * @param <H>     处理器类型
     * @param <R>     方法返回值类型
     * @return 可重复执行的链调用器
     */
    @Nonnull
    <H, R> Chain<H, R> createChain(@Nonnull Class<H> type, @Nonnull ChainInvocationFactory factory);

    /**
     * 使用调用处理器工厂创建链式代理。
     *
     * @param type    处理器接口类型
     * @param factory 调用处理器工厂，决定结果收集策略
     * @param <H>     处理器类型
     * @return 类型为 {@code H} 的动态代理实例
     */
    @Nonnull
    <H> H createChain(@Nonnull Class<H> type, @Nonnull InvocationHandlerFactory factory);

    /**
     * 创建「首个非空结果」策略的链式代理（正序执行）。
     *
     * @param type 处理器接口类型
     * @param <H>  处理器类型
     * @return 按优先级返回第一个非空结果的代理实例
     */
    @Nonnull
    default <H> H createFirstResultChain(@Nonnull Class<H> type) {
        return this.createFirstResultChain(type, false);
    }

    /**
     * 创建「首个非空结果」策略的链式代理。
     *
     * @param type      处理器接口类型
     * @param isReverse 是否逆序执行处理器（true 时低优先级优先）
     * @param <H>       处理器类型
     * @return 按优先级返回第一个非空结果的代理实例
     */
    @Nonnull
    default <H> H createFirstResultChain(@Nonnull Class<H> type, boolean isReverse) {
        return this.createChain(type, new FirstResultInvocationHandlerFactory(isReverse));
    }

    /**
     * 创建「最后一个结果」策略的链式代理（正序执行）。
     *
     * @param type 处理器接口类型
     * @param <H>  处理器类型
     * @return 返回最后一个处理器执行结果的代理实例
     */
    @Nonnull
    default <H> H createLastResultChain(@Nonnull Class<H> type) {
        return this.createLastResultChain(type, false);
    }

    /**
     * 创建「最后一个结果」策略的链式代理。
     *
     * @param type      处理器接口类型
     * @param isReverse 是否逆序执行处理器
     * @param <H>       处理器类型
     * @return 返回最后一个处理器执行结果的代理实例
     */
    @Nonnull
    default <H> H createLastResultChain(@Nonnull Class<H> type, boolean isReverse) {
        return this.createChain(type, new LastResultInvocationHandlerFactory(isReverse));
    }

    /**
     * 创建「忽略返回值」策略的链式代理（正序执行）。
     *
     * @param type 处理理器接口类型
     * @param <H>  处理器类型
     * @return 依次调用所有处理器但始终返回 null 的代理实例
     */
    @Nonnull
    default <H> H createNoneResultChain(@Nonnull Class<H> type) {
        return this.createNoneResultChain(type, false);
    }

    /**
     * 创建「忽略返回值」策略的链式代理。
     *
     * @param type      处理器接口类型
     * @param isReverse 是否逆序执行处理器
     * @param <H>       处理器类型
     * @return 依次调用所有处理器但始终返回 null 的代理实例
     */
    @Nonnull
    default <H> H createNoneResultChain(@Nonnull Class<H> type, boolean isReverse) {
        return this.createChain(type, new NoneResultInvocationHandlerFactory(isReverse));
    }
}
