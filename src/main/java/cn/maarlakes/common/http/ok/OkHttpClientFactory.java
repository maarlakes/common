package cn.maarlakes.common.http.ok;

import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientConfig;
import cn.maarlakes.common.http.HttpClientFactory;
import jakarta.annotation.Nonnull;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @author linjpxc
 */
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
                log.warn("OkHttp requires ExecutorService, wrapping Executor with an adapter. Consider providing an ExecutorService instead.");
                builder.dispatcher(new Dispatcher(new ExecutorServiceAdapter(executor)));
            }
        }
        return new OkAsyncHttpClient(builder.build(), config.getSslContext(), config);
    }

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
