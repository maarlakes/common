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
 * <h3>线程生命周期状态机</h3>
 * <pre>
 *   STOPPED ──CAS──▶ STARTED ──set──▶ DESTROY
 *      ▲                │                 │
 *      │                │                 │
 *      └────────────────┘                 │
 *         (线程退出时回到)                  (close() 调用)
 * </pre>
 * <ul>
 *   <li>{@code STOPPED}：初始状态，无线程运行</li>
 *   <li>{@code STARTED}：消费线程运行中</li>
 *   <li>{@code DESTROY}：队列已关闭，不再接受新监听器</li>
 * </ul>
 *
 * <h3>指数退避策略</h3>
 * <p>当 {@link #take()} 抛出异常时，消费线程不会退出，而是按指数退避策略等待后重试。
 * 初始等待 100ms，每次翻倍，上限 30 秒。成功获取消息后重置为 100ms。
 *
 * <h3>消息确认机制</h3>
 * <ul>
 *   <li>监听器可通过 {@link MessageContext#acknowledge()} 显式确认消息</li>
 *   <li>开启 autoAck 后，所有监听器无异常时自动确认</li>
 *   <li>未确认的消息通过 {@link #reOffer} 重新入队</li>
 * </ul>
 *
 * <h3>速率限制</h3>
 * <p>可选的 {@link RateLimiter} 集成。如果提供了 RateLimiter，消费线程在每次 {@code take()} 前会先获取许可，
 * 实现消息消费速率控制。{@code null} 表示不限制速率。
 *
 * @param <T> 消息类型
 * @author linjpxc
 */
public abstract class AbstractBlockingQueue<T> extends AbstractMessageQueue<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractBlockingQueue.class);

    /** 指数退避最大等待时间：30 秒 */
    private static final long MAX_BACKOFF_MS = 30_000L;

    /** 队列已关闭 */
    private static final int DESTROY = 0;
    /** 初始状态，消费线程未启动 */
    private static final int STOPPED = 1;
    /** 消费线程运行中 */
    private static final int STARTED = 2;

    protected final Executor executor;

    private final AtomicInteger status = new AtomicInteger(STOPPED);
    private final AtomicReference<Thread> mainThread = new AtomicReference<>();
    private final RateLimiter rateLimiter;
    private volatile boolean autoAck;

    /**
     * @param executor    用于异步执行监听器回调的执行器
     * @param rateLimiter 消息消费速率限制器，{@code null} 表示不限制
     */
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

    /**
     * 启动消费线程。使用 CAS 保证只有一个线程被创建，避免并发添加监听器时重复启动。
     *
     * <p>消费线程为守护线程（{@link Thread#setDaemon(boolean) daemon=true}），
     * 不会阻止 JVM 退出。线程命名为 {@code "queue-consumer-" + 队列名称}。
     */
    @SuppressWarnings("BusyWait")
    protected void start() {
        if (this.status.compareAndSet(STOPPED, STARTED)) {
            log.debug("队列 {} 消费线程启动", this.name());
            final Thread thread = new Thread(() -> {
                long backoffMs = 100L;
                while (this.status.get() == STARTED) {
                    try {
                        // 速率限制：获取许可后再取消息
                        if (this.rateLimiter != null) {
                            this.rateLimiter.acquire();
                        }
                        // CAS 成功后 rateLimiter.acquire() 可能阻塞，再次检查状态
                        if (this.status.get() != STARTED) {
                            break;
                        }
                        final T msg = this.take();
                        final DefaultMessageContext<T> context = new DefaultMessageContext<>(
                                this.name(), msg, () -> this.reOffer(msg)
                        );
                        // 成功获取消息，重置退避时间
                        backoffMs = 100L;
                        if (log.isTraceEnabled()) {
                            log.trace("队列 {} 获取到消息: {}", this.name(), msg);
                        }
                        this.executor.execute(() -> {
                            boolean success = this.dispatchMessage(context);
                            context.complete(success, this.autoAck);
                        });
                    } catch (InterruptedException e) {
                        // 中断信号：close() 或 destroyMainThread() 触发，正常退出循环
                        break;
                    } catch (Exception e) {
                        log.warn("从队列 {} 获取消息失败，{}ms 后重试", this.name(), backoffMs, e);
                        try {
                            Thread.sleep(backoffMs);
                            // 指数退避：每次翻倍，上限 30 秒
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
        // 首个监听器注册时启动消费线程
        if (this.listeners.size() == 1) {
            log.debug("队列 {} 首个监听器已注册，启动消费线程", this.name());
            this.start();
        }
    }

    @Override
    public synchronized void removeListener(@Nonnull QueueListener<T> listener) {
        super.removeListener(listener);
    }

    /**
     * 阻塞获取一条消息。由消费线程循环调用，队列无消息时阻塞等待。
     *
     * @return 获取到的消息，不为 null
     * @throws Exception 获取消息时发生异常（如连接中断）
     */
    @Nonnull
    protected abstract T take() throws Exception;

    /**
     * 将未确认的消息重新投递到队列。在监听器处理完成且消息未被确认时调用。
     *
     * @param value 要重新入队的消息
     */
    protected abstract void reOffer(@Nonnull T value);

    /**
     * 销毁消费线程。通过 CAS 清空线程引用并发送中断信号。
     */
    private void destroyMainThread() {
        final Thread thread = mainThread.get();
        if (thread != null) {
            mainThread.compareAndSet(thread, null);
            thread.interrupt();
        }
    }
}
