package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author linjpxc
 */
public interface EventPublisherFactory {

//    @Nonnull
//    EventPublisher getPublisher();

    @Nonnull
    EventPublisher createPublisher(@Nonnull EventDispatcher dispatcher, @Nonnull List<? extends EventInvoker> invokers);
}
