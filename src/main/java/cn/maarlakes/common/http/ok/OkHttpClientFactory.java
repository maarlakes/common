package cn.maarlakes.common.http.ok;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientConfig;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 基于 OkHttp 的 {@link HttpClientFactory} SPI 实现。
 *
 * <p>优先级 {@code @Order(100)}，在所有 SPI 工厂中最高，因此 classpath 上有 OkHttp 时优先使用。
 * OkHttp 原生支持异步调用（通过 {@code Call.enqueue}），适配层较薄。
 *
 * <p>需要 {@code com.squareup.okhttp3:okhttp} 在 classpath 上，
 * 否则 {@link #createClient} 会抛出异常，SPI 加载器会跳过此工厂。
 *
 * <p>注意：OkHttp 的 {@link Dispatcher} 要求 {@link ExecutorService}，
 * 如果配置中只提供了 {@link Executor}，会通过 {@link ExecutorServiceAdapter} 适配并输出 WARN 日志。
 *
 * @author linjpxc
 */
@Order(100)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class OkHttpClientFactory implements HttpClientFactory {
    private static final Logger log = LoggerFactory.getLogger(OkHttpClientFactory.class);

    @Nonnull
    @Override
    public HttpClient createClient(@Nonnull HttpClientConfig config) {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        final Executor executor = config.getExecutor();
        if (executor != null) {
            if (executor instanceof ExecutorService) {
                builder.dispatcher(new Dispatcher((ExecutorService) executor));
            } else {
                // OkHttp Dispatcher 只接受 ExecutorService，需要适配
                log.warn("OkHttp 需要 ExecutorService, 正在使用适配器包装 Executor. 建议直接提供 ExecutorService.");
                builder.dispatcher(new Dispatcher(new ExecutorServiceAdapter(executor)));
            }
        }
        log.info("已创建 OkHttp 客户端, SSL 已启用: {}", config.getSslContext() != null);
        return new OkAsyncHttpClient(builder.build(), config.getSslContext(), config);
    }

    /**
     * 将 {@link Executor} 适配为 {@link ExecutorService} 的桥接类。
     *
     * <p>仅实现最小接口以配合 OkHttp 的 {@link Dispatcher}。
     * {@code shutdown} 状态是虚设的——实际线程池的生命周期由调用方管理。
     */
    private static class ExecutorServiceAdapter extends AbstractExecutorService {

        private final Executor executor;
        private volatile boolean shutdown;

        private ExecutorServiceAdapter(Executor executor) {
            this.executor = executor;
        }

        @Override
        public void shutdown() {
            this.shutdown = true;
        }

        @Override
        public java.util.List<Runnable> shutdownNow() {
            this.shutdown = true;
            return java.util.Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return this.shutdown;
        }

        @Override
        public boolean isTerminated() {
            return this.shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            if (!this.shutdown) {
                this.executor.execute(command);
            }
        }
    }
}
