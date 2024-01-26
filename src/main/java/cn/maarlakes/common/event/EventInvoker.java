package cn.maarlakes.common.event;

import cn.maarlakes.common.Ordered;
import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface EventInvoker extends Ordered {

    boolean supportedAsync();

    boolean supportedEvent(@Nonnull Class<?> eventType);

    <E> void invoke(@Nonnull E event);
}
