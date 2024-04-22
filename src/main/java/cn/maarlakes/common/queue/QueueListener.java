package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.util.EventListener;

/**
 * @author linjpxc
 */
public interface QueueListener<T> extends EventListener {

    void onTake(@Nonnull QueueContext<T> context);
}
