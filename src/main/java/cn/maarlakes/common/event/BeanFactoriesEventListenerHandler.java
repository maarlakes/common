package cn.maarlakes.common.event;

import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author linjpxc
 */
final class BeanFactoriesEventListenerHandler {

    @SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
    public static class Handler implements EventListenerHandler {
        @Override
        public <L> List<EventInvoker> getInvokers(@Nonnull L listener) {
            final List<EventInvoker> invokers = new ArrayList<>();
            for (Method method : listener.getClass().getDeclaredMethods()) {
                final EventListener eventListener = method.getAnnotation(EventListener.class);
                if (eventListener == null) {
                    continue;
                }
                if (method.getParameterCount() < 1) {
                    throw new IllegalArgumentException("Method <" + method + "> of class <" + method.getDeclaringClass() + "> " +
                            "is annotated with <" + EventListener.class.getName() + "> but has 0  parameters! " +
                            "Listener methods MUST have 1 parameter.");
                }
                invokers.add(new BeanFactoriesEventInvoker(listener, method, eventListener));
            }
            return Collections.unmodifiableList(invokers);
        }
    }
}
