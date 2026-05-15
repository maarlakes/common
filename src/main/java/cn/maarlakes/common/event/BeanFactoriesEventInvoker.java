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
 * @author linjpxc
 */
class BeanFactoriesEventInvoker implements EventInvoker, Ordered {
    private static final Logger log = LoggerFactory.getLogger(BeanFactoriesEventInvoker.class);

    private final Object listener;
    private final Method method;
    private final EventListener eventListener;

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
        A annotation = this.method.getAnnotation(annotationType);
        if (annotation == null) {
            annotation = this.method.getDeclaringClass().getAnnotation(annotationType);
        }
        return annotation;
    }

    @Override
    public void invoke(@Nonnull Object event) {
        if (this.method.getParameterCount() < 1) {
            throw new EventException(this.method.getName() + " Unsupported event type: " + event.getClass());
        }

        final String methodDesc = this.method.getDeclaringClass().getSimpleName() + "." + this.method.getName();
        if (log.isDebugEnabled()) {
            log.debug("Invoking event listener: {}", methodDesc);
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
                log.trace("Resolved args for {}: {}", methodDesc, java.util.Arrays.toString(args));
            }
            this.invokeMethod(args);
        }
    }

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
            throw new EventException("Unsupported event type: " + eventType);
        }
        return eventIndex;
    }

    @Override
    public boolean supportedEvent(@Nonnull Class<?> eventType) {
        final String methodDesc = this.method.getDeclaringClass().getSimpleName() + "." + this.method.getName();
        if (this.eventListener.events().length > 0) {
            for (Class<?> type : this.eventListener.events()) {
                if (type == eventType) {
                    // 当类型匹配时，判断实际入参是否匹配
                    for (Class<?> parameterType : this.method.getParameterTypes()) {
                        if (parameterType == eventType || parameterType.isAssignableFrom(eventType)) {
                            if (log.isDebugEnabled()) {
                                log.debug("Listener {} supports event {} (matched via @EventListener.events)", methodDesc, eventType.getName());
                            }
                            return true;
                        }
                    }
                }
            }
            if (log.isDebugEnabled()) {
                log.debug("Listener {} does not support event {} (subscription mismatch: subscribed to {}, parameters: {})",
                        methodDesc, eventType.getName(), this.eventListener.events(), this.method.getParameterTypes());
            }
            return false;
        }

        for (Class<?> parameterType : this.method.getParameterTypes()) {
            if (parameterType.isAssignableFrom(eventType)) {
                if (log.isDebugEnabled()) {
                    log.debug("Listener {} supports event {} (parameter {} is assignable)", methodDesc, eventType.getName(), parameterType.getName());
                }
                return true;
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Listener {} does not support event {} (no matching parameter)", methodDesc, eventType.getName());
        }
        return false;
    }

    private void invokeMethod(@Nonnull Object... args) {
        try {
            this.method.invoke(this.listener, args);
        } catch (InvocationTargetException e) {
            throw new EventException(e.getTargetException());
        } catch (Exception e) {
            throw new EventException(e);
        }
    }

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
