package cn.maarlakes.common.queue;

import cn.maarlakes.common.utils.RateLimiter;
import jakarta.annotation.Nonnull;

import java.io.Closeable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author linjpxc
 */
public abstract class AbstractBlockingQueue<T> extends AbstractTopicQueue<T> implements Closeable {

    protected static final int DESTROY = 0;
    protected static final int STOPPED = 1;
    protected static final int STARTING = 2;
    protected static final int STARTED = 3;
    protected final Executor executor;

    protected final AtomicInteger status = new AtomicInteger(STOPPED);
    private final AtomicReference<Thread> mainThread = new AtomicReference<>();
    private final RateLimiter rateLimiter;

    protected AbstractBlockingQueue(Executor executor, RateLimiter rateLimiter) {
        this.executor = executor;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void close() {
        this.status.set(DESTROY);
        this.destroyMainThread();
    }

    protected void start() {
        if (this.status.compareAndSet(STOPPED, STARTED)) {
            final Thread thread = new Thread(() -> {
                while (this.status.get() == STARTED) {
                    try {
                        if (this.rateLimiter != null) {
                            this.rateLimiter.acquire();
                        }
                        if (this.status.get() != STARTED) {
                            break;
                        }
                        final DefaultQueueContext<T> context = new DefaultQueueContext<>(this.name(), this.take());
                        this.executor.execute(() -> this.onMessage(context));
                    } catch (InterruptedException e) {
                        this.status.compareAndSet(STARTED, STOPPED);
                        break;
                    } catch (Exception ignored) {
                    }
                }
            });
            thread.setDaemon(true);
            mainThread.set(thread);
            thread.start();
        }
    }

    protected void stop() {
        if (this.status.compareAndSet(STARTED, STOPPED)) {
            this.destroyMainThread();
        }
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

    private void destroyMainThread() {
        final Thread thread = mainThread.get();
        if (thread != null) {
            mainThread.compareAndSet(thread, null);
            try {
                thread.interrupt();
            } catch (Exception ignored) {
            }
        }
    }
}
