package cn.maarlakes.common.http;

import cn.maarlakes.common.http.jdk.JdkHttpClient;
import cn.maarlakes.common.spi.SpiServiceLoader;
import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

/**
 * @author linjpxc
 */
public final class HttpClients {

    private static volatile HttpClient CLIENT;
    private static HttpClientFactory FACTORY;
    private static final Set<Class<?>> EXCLUDES = new HashSet<>();
    private static final Object LOCK = new Object();

    private HttpClients() {
    }

    @Nonnull
    public static CompletableFuture<Response> execute(@Nonnull Request request) {
        return doExecute(client -> client.execute(request));
    }

    @Nonnull
    public static CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config) {
        return doExecute(client -> client.execute(request, config));
    }

    @Nonnull
    public static <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler) {
        return doExecute(client -> client.execute(request, config, handler));
    }

    @Nonnull
    public static <T> CompletableFuture<T> execute(@Nonnull Request request, @Nonnull ResponseHandler<T> handler) {
        return doExecute(client -> client.execute(request, null, handler));
    }

    private static <T> CompletableFuture<T> doExecute(HttpClientExecutor<T> executor) {
        try {
            return executor.execute(getDefault());
        } catch (NoClassDefFoundError e) {
            synchronized (LOCK) {
                final HttpClientFactory factory = FACTORY;
                if (factory != null) {
                    EXCLUDES.add(factory.getClass());
                }
                CLIENT = null;
                FACTORY = null;
            }
            try {
                return executor.execute(getDefault());
            } catch (NoClassDefFoundError ex) {
                final CompletableFuture<T> future = new CompletableFuture<>();
                future.completeExceptionally(ex);
                return future;
            }
        }
    }

    @Nonnull
    private static HttpClient getDefault() {
        synchronized (LOCK) {
            if (CLIENT == null) {
                try {
                    final HttpClientConfig config = HttpClientConfig.builder().executor(new ForkJoinPool()).build();
                    for (HttpClientFactory factory : SpiServiceLoader.loadShared(HttpClientFactory.class)) {
                        if (EXCLUDES.contains(factory.getClass())) {
                            continue;
                        }
                        try {
                            CLIENT = factory.createClient(config);
                            FACTORY = factory;
                            break;
                        } catch (Exception ignored) {
                            EXCLUDES.add(factory.getClass());
                        }
                    }
                } catch (Exception ignored) {
                }
                if (CLIENT == null) {
                    CLIENT = new JdkHttpClient(new ForkJoinPool());
                    FACTORY = null;
                }
            }
            return CLIENT;
        }
    }

    @FunctionalInterface
    private interface HttpClientExecutor<T> {
        CompletableFuture<T> execute(HttpClient client);
    }
}
