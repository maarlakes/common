package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * 事件上下文接口，提供键值对属性存取能力。
 *
 * <p><b>设计意图</b>
 * <p>这是一个提供给使用者的便携类（mixin/混入接口）。当业务系统定义自己的事件类时，
 * 可以直接让该事件类实现此接口（或继承 {@link DefaultEventContext}），从而在事件对象
 * 内部携带上下文属性（如追踪 ID、租户信息、用户会话等），随事件一起流转。
 *
 * <p>本接口本身<b>不会</b>被事件总线的核心流程（{@link EventDispatcher}、
 * {@link EventPublisher}、{@link EventInvoker}）主动引用或注入，它完全由使用方
 * 在自定义事件类型中按需继承使用。
 *
 * @author linjpxc
 * @see DefaultEventContext
 */
public interface EventContext extends Serializable {

    <K, V> void setAttribute(@Nonnull K key, V value);

    <K, V> V getAttribute(@Nonnull K key);
}
