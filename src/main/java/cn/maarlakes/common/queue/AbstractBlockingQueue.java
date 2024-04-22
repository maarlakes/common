package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author linjpxc
 */
public abstract class AbstractBlockingQueue<T> extends AbstractTopicQueue<T> implements Closeable {

    protected static final int DESTROY = 0;
    protected static final int STOPPED = 1;
    protected static final int STARTED = 2;
    protected final Executor executor;

    protected final AtomicInteger status = new AtomicInteger(STOPPED);

    protected AbstractBlockingQueue(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void close() {
        this.status.set(DESTROY);
    }

    protected void start() {
        if (this.status.compareAndSet(STOPPED, STARTED)) {
            final Thread thread = new Thread(() -> {
                while (this.status.get() == STARTED) {
                    try {
                        final DefaultQueueContext<T> context = new DefaultQueueContext<>(this.name(), this.take());
                        CompletableFuture.runAsync(() -> this.onMessage(context), this.executor);
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception ignored) {
                    }
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
    }

    protected void stop() {
        this.status.set(STOPPED);
    }

    @Override
    public void addListener(@Nonnull QueueListener<T> listener) {
        super.addListener(listener);
        if (!this.listeners.isEmpty()) {
            this.start();
        }
    }

    @Override
    public void removeListener(@Nonnull QueueListener<T> listener) {
        super.removeListener(listener);
        if (this.listeners.isEmpty()) {
            this.stop();
        }
    }

    @Nonnull
    protected abstract T take() throws Exception;

}
