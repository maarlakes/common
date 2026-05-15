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
 * @author linjpxc
 */
final class BeanFactoriesEventListenerHandler {
    private static final Logger log = LoggerFactory.getLogger(BeanFactoriesEventListenerHandler.class);

    @SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
    public static class Handler implements EventListenerHandler {

        @Override
        public <L> List<EventInvoker> getInvokers(@Nonnull L listener) {
            final List<EventInvoker> invokers = new ArrayList<>();
            final Set<String> signatures = new HashSet<>();
            Class<?> clazz = listener.getClass();
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
                        throw new IllegalArgumentException("Method <" + method + "> of class <" + method.getDeclaringClass() + "> " +
                                "is annotated with <" + EventListener.class.getName() + "> but has 0  parameters! " +
                                "Listener methods MUST have 1 parameter.");
                    }
                    final String signature = method.getName() + Arrays.toString(method.getParameterTypes());
                    if (signatures.add(signature)) {
                        invokers.add(new BeanFactoriesEventInvoker(listener, method, eventListener));
                        log.debug("Discovered event listener method: {}.{} in listener {}",
                                method.getDeclaringClass().getSimpleName(), method.getName(), listener.getClass().getName());
                    }
                }
                clazz = clazz.getSuperclass();
            }
            if (log.isDebugEnabled() && !invokers.isEmpty()) {
                log.debug("Resolved {} event invoker(s) from listener {}", invokers.size(), listener.getClass().getName());
            }
            return Collections.unmodifiableList(invokers);
        }
    }
}
