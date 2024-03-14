package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface EventPublisherFactory {

    @Nonnull
    EventPublisher getPublisher();
}
