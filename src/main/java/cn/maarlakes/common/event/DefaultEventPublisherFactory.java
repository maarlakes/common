package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.List;

public class DefaultEventPublisherFactory implements EventPublisherFactory {

    @Nonnull
    @Override
    public EventPublisher createPublisher(@Nonnull EventDispatcher dispatcher, @Nonnull List<? extends EventInvoker> invokers) {
        return new DefaultEventPublisher(dispatcher, invokers);
    }
}
