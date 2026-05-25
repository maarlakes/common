package cn.maarlakes.common.event;

import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 基于 {@link BeanFactories} 的事件监听器处理器，扫描监听器对象中标注了 {@link EventListener} 的方法。
 *
 * <p><b>方法发现策略：</b>
 * <ul>
 *   <li>从监听器实际类型开始，沿类继承链向上遍历直到 {@link Object}</li>
 *   <li>跳过 bridge 方法和 synthetic 方法</li>
 *   <li>按 {@code 方法名 + 参数类型列表} 去重，避免子类覆盖父类方法时重复注册</li>
 *   <li>被 {@link EventListener} 标注的方法必须至少有一个参数，否则抛出异常</li>
 * </ul>
 *
 * <p>内部类 {@link Handler} 通过 SPI 注册为单例服务。
 *
 * @author linjpxc
 */
final class BeanFactoriesEventListenerHandler {
    private static final Logger log = LoggerFactory.getLogger(BeanFactoriesEventListenerHandler.class);

    /**
     * SPI 单例处理器，从监听器对象中提取所有事件调用器。
     */
    @SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
    public static class Handler implements EventListenerHandler {

        @Override
        public <L> List<EventInvoker> getInvokers(@Nonnull L listener) {
            final List<EventInvoker> invokers = new ArrayList<>();
            final Set<String> signatures = new HashSet<>();
            Class<?> clazz = listener.getClass();
            // 沿类继承链向上扫描
            while (clazz != null && clazz != Object.class) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isBridge() || method.isSynthetic()) {
                        continue;
                    }
                    final EventListener eventListener = method.getAnnotation(EventListener.class);
                    if (eventListener == null) {
                        continue;
                    }
                    if (method.getParameterCount() < 1) {
                        throw new IllegalArgumentException("方法 <" + method + "> (类 <" + method.getDeclaringClass() + ">) " +
                                "标注了 <" + EventListener.class.getName() + "> 但没有参数！监听方法必须至少有 1 个参数。");
                    }
                    // 按方法签名去重
                    final String signature = method.getName() + Arrays.toString(method.getParameterTypes());
                    if (signatures.add(signature)) {
                        invokers.add(new BeanFactoriesEventInvoker(listener, method, eventListener));
                        log.debug("发现事件监听方法: {}.{} (监听器: {})",
                                method.getDeclaringClass().getSimpleName(), method.getName(), listener.getClass().getName());
                    }
                }
                clazz = clazz.getSuperclass();
            }
            if (log.isDebugEnabled() && !invokers.isEmpty()) {
                log.debug("从监听器 {} 中解析出 {} 个事件调用器", listener.getClass().getName(), invokers.size());
            }
            return Collections.unmodifiableList(invokers);
        }
    }
}
