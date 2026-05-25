package cn.maarlakes.common.event;

import cn.maarlakes.common.AnnotationOrderComparator;
import cn.maarlakes.common.Ordered;
import cn.maarlakes.common.factory.bean.BeanFactories;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * 基于 {@link BeanFactories} 的事件调用器实现，支持多参数监听器方法的依赖注入。
 *
 * <p>对于单参数方法，直接将事件传入；对于多参数方法，先确定事件参数的位置（通过类型匹配），
 * 其余参数通过 {@link BeanFactories#getBeanOrNull} 从容器中获取。
 *
 * <p>实现了 {@link Ordered} 接口，通过方法上的 {@code @Order} 注解确定调用顺序。
 *
 * @author linjpxc
 */
class BeanFactoriesEventInvoker implements EventInvoker, Ordered {
    private static final Logger log = LoggerFactory.getLogger(BeanFactoriesEventInvoker.class);

    /** 监听器目标对象 */
    private final Object listener;
    /** 监听器方法 */
    private final Method method;
    /** 方法上的 @EventListener 注解 */
    private final EventListener eventListener;

    /**
     * 创建事件调用器。
     *
     * @param listener      监听器对象实例
     * @param method        监听器方法（将被设置为可访问）
     * @param eventListener 方法上的 @EventListener 注解
     */
    public BeanFactoriesEventInvoker(@Nonnull Object listener, @Nonnull Method method, @Nonnull EventListener eventListener) {
        this.listener = listener;
        this.method = method;
        this.eventListener = eventListener;
        AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
            method.setAccessible(true);
            return null;
        });
    }

    @Override
    public <A extends Annotation> A getAnnotation(@Nonnull Class<A> annotationType) {
        // 优先从方法上查找，再从声明类上查找
        A annotation = this.method.getAnnotation(annotationType);
        if (annotation == null) {
            annotation = this.method.getDeclaringClass().getAnnotation(annotationType);
        }
        return annotation;
    }

    /**
     * 调用监听器方法处理事件。
     *
     * <p>单参数方法直接传入事件；多参数方法先定位事件参数位置，其余参数通过
     * {@link BeanFactories} 注入。参数定位策略：先精确类型匹配（{@code ==}），
     * 再使用 {@code isAssignableFrom} 兼容匹配。
     */
    @Override
    public void invoke(@Nonnull Object event) {
        if (this.method.getParameterCount() < 1) {
            throw new EventException(this.method.getName() + " Unsupported event type: " + event.getClass());
        }

        final String methodDesc = this.method.getDeclaringClass().getSimpleName() + "." + this.method.getName();
        if (log.isDebugEnabled()) {
            log.debug("调用事件监听器: {}", methodDesc);
        }

        if (method.getParameterCount() == 1) {
            this.invokeMethod(event);
        } else {
            final Class<?> eventType = event.getClass();
            final Class<?>[] parameterTypes = method.getParameterTypes();

            final int eventIndex = getEventIndex(parameterTypes, eventType);

            final Object[] args = new Object[method.getParameterCount()];
            for (int i = 0; i < parameterTypes.length; i++) {
                args[i] = (i == eventIndex) ? event : BeanFactories.getBeanOrNull(parameterTypes[i]);
            }
            if (log.isTraceEnabled()) {
                log.trace("监听器 {} 参数解析结果: {}", methodDesc, java.util.Arrays.toString(args));
            }
            this.invokeMethod(args);
        }
    }

    /**
     * 在方法参数类型数组中定位事件参数的位置。
     *
     * <p>匹配策略：先精确匹配（{@code ==}），再兼容匹配（{@code isAssignableFrom}）。
     * 这确保了当方法参数为 {@code Object} 时也能接收任何事件类型。
     *
     * @param parameterTypes 方法参数类型数组
     * @param eventType      事件类型
     * @return 事件参数在参数数组中的索引
     * @throws EventException 如果无法匹配到事件参数
     */
    private static int getEventIndex(Class<?>[] parameterTypes, Class<?> eventType) {
        int eventIndex = -1;
        for (int i = 0; i < parameterTypes.length; i++) {
            if (parameterTypes[i] == eventType) {
                eventIndex = i;
                break;
            }
        }
        if (eventIndex < 0) {
            for (int i = 0; i < parameterTypes.length; i++) {
                if (parameterTypes[i].isAssignableFrom(eventType)) {
                    eventIndex = i;
                    break;
                }
            }
        }
        if (eventIndex < 0) {
            throw new EventException("不支持的事件类型: " + eventType);
        }
        return eventIndex;
    }

    /**
     * 判断本调用器是否支持处理指定类型的事件。
     *
     * <p>匹配规则：
     * <ol>
     *   <li>若 {@code @EventListener.events()} 非空，事件类型必须在列表中，且方法参数类型兼容</li>
     *   <li>若 {@code @EventListener.events()} 为空，只要方法任一参数类型能接受该事件即可</li>
     * </ol>
     */
    @Override
    public boolean supportedEvent(@Nonnull Class<?> eventType) {
        final String methodDesc = this.method.getDeclaringClass().getSimpleName() + "." + this.method.getName();
        if (this.eventListener.events().length > 0) {
            for (Class<?> type : this.eventListener.events()) {
                if (type == eventType) {
                    // 当类型匹配时，判断实际入参是否匹配
                    for (Class<?> parameterType : this.method.getParameterTypes()) {
                        if (parameterType == eventType || parameterType.isAssignableFrom(eventType)) {
                            if (log.isTraceEnabled()) {
                                log.trace("监听器 {} 支持事件 {} (通过 @EventListener.events 匹配)", methodDesc, eventType.getName());
                            }
                            return true;
                        }
                    }
                }
            }
            if (log.isTraceEnabled()) {
                log.trace("监听器 {} 不支持事件 {} (订阅不匹配: 订阅了 {}, 方法参数: {})",
                        methodDesc, eventType.getName(), this.eventListener.events(), this.method.getParameterTypes());
            }
            return false;
        }

        for (Class<?> parameterType : this.method.getParameterTypes()) {
            if (parameterType.isAssignableFrom(eventType)) {
                if (log.isTraceEnabled()) {
                    log.trace("监听器 {} 支持事件 {} (参数 {} 兼容)", methodDesc, eventType.getName(), parameterType.getName());
                }
                return true;
            }
        }
        if (log.isTraceEnabled()) {
            log.trace("监听器 {} 不支持事件 {} (无匹配参数)", methodDesc, eventType.getName());
        }
        return false;
    }

    /**
     * 通过反射调用监听器方法。
     *
     * <p>将 {@link InvocationTargetException} 解包为原始异常后包装为 {@link EventException} 抛出。
     */
    private void invokeMethod(@Nonnull Object... args) {
        try {
            this.method.invoke(this.listener, args);
        } catch (InvocationTargetException e) {
            throw new EventException(e.getTargetException());
        } catch (Exception e) {
            throw new EventException(e);
        }
    }

    /**
     * 获取调用器的排序值，从方法上的 {@code @Order} 注解读取。
     * 未标注时返回 {@link Ordered#LOWEST}（最低优先级）。
     */
    @Override
    public int order() {
        final Integer order = AnnotationOrderComparator.findOrder(this.method);
        return order == null ? Ordered.LOWEST : order;
    }

    @Override
    public String toString() {
        return this.method.getDeclaringClass().getSimpleName() + "." + this.method.getName();
    }
}
