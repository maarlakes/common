package cn.maarlakes.common.event;

import cn.maarlakes.common.AnnotationOrderComparator;
import cn.maarlakes.common.Ordered;
import cn.maarlakes.common.factory.bean.BeanFactories;
import jakarta.annotation.Nonnull;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * @author linjpxc
 */
class BeanFactoriesEventInvoker implements EventInvoker, Ordered {

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
    public boolean supportedAsync() {
        return this.eventListener.async();
    }

    @Override
    public void invoke(@Nonnull Object event) {
        if (method.getParameterCount() == 1) {
            this.invokeMethod(event);
        } else {
            // 多参数
            final Object[] args = new Object[method.getParameterCount()];
            final Class<?>[] parameterTypes = method.getParameterTypes();
            final Class<?> eventType = event.getClass();
            int index = -1;
            Class<?> backType = null;
            for (int i = 0; i < parameterTypes.length; i++) {
                final Class<?> type = parameterTypes[i];
                if (type == eventType) {
                    args[i] = event;
                    if (index >= 0 && backType != null) {
                        args[index] = BeanFactories.getBeanOrNull(backType);
                        backType = null;
                    }
                    index = i;
                } else {
                    if (args[i] == null && type.isAssignableFrom(eventType)) {
                        args[i] = event;
                        index = i;
                        backType = type;
                    } else {
                        args[i] = BeanFactories.getBeanOrNull(type);
                    }
                }
            }
            if (index < 0) {
                throw new EventException("Unsupported event type: " + eventType);
            }
            this.invokeMethod(args);
        }
    }

    @Override
    public boolean supportedEvent(@Nonnull Class<?> eventType) {
        for (Class<?> type : this.eventListener.events()) {
            if (type == eventType) {
                return true;
            }
        }
        for (Class<?> parameterType : this.method.getParameterTypes()) {
            if (parameterType == eventType) {
                return true;
            }
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
}
