package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * 事件监听器处理器，负责从监听器对象中提取所有可用的 {@link EventInvoker}。
 *
 * <p>实现类通过反射或其他机制扫描监听器对象中标注了 {@link EventListener} 的方法，
 * 为每个方法创建对应的 {@link EventInvoker} 实例。
 *
 * @author linjpxc
 * @see BeanFactoriesEventListenerHandler
 */
public interface EventListenerHandler {

    /**
     * 从监听器对象中提取所有事件调用器。
     *
     * @param <L>       监听器类型
     * @param listener  监听器对象
     * @return 该监听器中所有事件监听方法对应的调用器列表，可能为空列表
     */
    <L> List<EventInvoker> getInvokers(@Nonnull L listener);
}
