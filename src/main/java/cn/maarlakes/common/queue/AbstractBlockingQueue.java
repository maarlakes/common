package cn.maarlakes.common.queue;

import cn.maarlakes.common.utils.RateLimiter;
import jakarta.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 阻塞队列抽象基类，管理消费线程生命周期和消息确认机制。
 *
 * <p>消费线程模型：首个监听器注册后自动启动守护线程，通过 {@link #take()} 阻塞获取消息，
 * 提交到 {@link Executor} 异步分发给监听器。
 *
 * <p>消息确认机制：
 * <ul>
 *   <li>监听器可通过 {@link MessageContext#acknowledge()} 显式确认消息</li>
 *   <li>开启 autoAck 后，所有监听器无异常时自动确认</li>
 *   <li>未确认的消息通过 {@link #reOffer} 重新入队</li>
 * </ul>
 *
 * <p>异常处理：take() 失败时采用指数退避策略（100ms → 30s）重试。
 *
 * @author linjpxc
 */
public abstract class AbstractBlockingQueue<T> extends AbstractMessageQueue<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractBlockingQueue.class);
    private static final long MAX_BACKOFF_MS = 30_000L;

    private static final int DESTROY = 0;
    private static final int STOPPED = 1;
    private static final int STARTED = 2;

    protected final Executor executor;

    private final AtomicInteger status = new AtomicInteger(STOPPED);
    private final AtomicReference<Thread> mainThread = new AtomicReference<>();
    private final RateLimiter rateLimiter;
    private volatile boolean autoAck;

    protected AbstractBlockingQueue(Executor executor, RateLimiter rateLimiter) {
        this.executor = executor;
        this.rateLimiter = rateLimiter;
    }

    @Override
    public boolean isAutoAck() {
        return this.autoAck;
    }

    @Override
    public void setAutoAck(boolean autoAck) {
        if (log.isDebugEnabled()) {
            log.debug("队列 {} autoAck 设置为 {}", this.name(), autoAck);
        }
        this.autoAck = autoAck;
    }

    @Override
    public void close() {
        if (log.isDebugEnabled()) {
            log.debug("队列 {} 关闭", this.name());
        }
        this.status.set(DESTROY);
        this.destroyMainThread();
    }

    @SuppressWarnings("BusyWait")
    protected void start() {
        if (this.status.compareAndSet(STOPPED, STARTED)) {
            log.debug("队列 {} 消费线程启动", this.name());
            final Thread thread = new Thread(() -> {
                long backoffMs = 100L;
                while (this.status.get() == STARTED) {
                    try {
                        if (this.rateLimiter != null) {
                            this.rateLimiter.acquire();
                        }
                        if (this.status.get() != STARTED) {
                            break;
                        }
                        final T msg = this.take();
                        final DefaultMessageContext<T> context = new DefaultMessageContext<>(
                                this.name(), msg, () -> this.reOffer(msg)
                        );
                        backoffMs = 100L;
                        this.executor.execute(() -> {
                            boolean success = this.dispatchMessage(context);
                            context.complete(success, this.autoAck);
                        });
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        log.warn("从队列 {} 获取消息失败，{}ms 后重试", this.name(), backoffMs, e);
                        try {
                            Thread.sleep(backoffMs);
                            backoffMs = Math.min(backoffMs * 2, MAX_BACKOFF_MS);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                }
                log.debug("队列 {} 消费线程退出", this.name());
            });
            thread.setDaemon(true);
            thread.setName("queue-consumer-" + this.name());
            mainThread.set(thread);
            thread.start();
        }
    }

    @Override
    public synchronized void addListener(@Nonnull QueueListener<T> listener) {
        if (this.status.get() == DESTROY) {
            throw new IllegalStateException("Queue " + this.name() + " has been closed");
        }
        super.addListener(listener);
        if (this.listeners.size() == 1) {
            this.start();
        }
    }

    @Override
    public synchronized void removeListener(@Nonnull QueueListener<T> listener) {
        super.removeListener(listener);
    }

    /** 阻塞获取一条消息 */
    @Nonnull
    protected abstract T take() throws Exception;

    /** 将未确认的消息重新投递到队列 */
    protected abstract void reOffer(@Nonnull T value);

    private void destroyMainThread() {
        final Thread thread = mainThread.get();
        if (thread != null) {
            mainThread.compareAndSet(thread, null);
            thread.interrupt();
        }
    }
}
