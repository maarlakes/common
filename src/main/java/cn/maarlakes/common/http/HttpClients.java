package cn.maarlakes.common.http;

import cn.maarlakes.common.http.jdk.JdkHttpClient;
import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.Lazy;
import cn.maarlakes.common.utils.NamedThreadFactory;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public final class HttpClients {
    private HttpClients() {
    }

    private static final AtomicInteger ID = new AtomicInteger(0);

    private static final Supplier<HttpClient> DEFAULT_CLIENT_FACTORY = Lazy.of(HttpClients::defaultClient);
    private static final List<HttpClientFactory> HTTP_CLIENT_FACTORIES;

    static {
        HTTP_CLIENT_FACTORIES = SpiServiceLoader.loadShared(HttpClientFactory.class, HttpClientFactory.class.getClassLoader())
                .stream().filter(item -> {
                    try {
                        return item.isAvailable();
                    } catch (NoClassDefFoundError ignored) {
                        return false;
                    }
                }).collect(Collectors.toList());
    }

    @Nonnull
    public static HttpClient defaultClient() {
        return createClientWithFallback(null);
    }

    @Nonnull
    public static HttpClient createClient() {
        return createClient(new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 4, 1, TimeUnit.MINUTES, new SynchronousQueue<>(), new NamedThreadFactory("maarlakes-http-client-" + ID.incrementAndGet() + "-")));
    }

    @Nonnull
    public static HttpClient createClient(@Nonnull Executor executor) {
        return createClientWithFallback(executor);
    }

    @Nonnull
    public static HttpClient createClient(@Nonnull HttpClientInterceptor... interceptors) {
        return createClient((Executor) null, interceptors);
    }

    @Nonnull
    public static HttpClient createClient(Executor executor, @Nonnull HttpClientInterceptor... interceptors) {
        final HttpClient client = createClientWithFallback(executor);
        if (interceptors == null || interceptors.length == 0) {
            return client;
        }
        return new InterceptableHttpClient(client, java.util.Arrays.asList(interceptors));
    }

    @Nonnull
    private static HttpClient createClientWithFallback(Executor executor) {
        for (HttpClientFactory factory : HTTP_CLIENT_FACTORIES) {
            try {
                if (executor != null) {
                    return factory.createClient(executor);
                }
                return factory.createClient();
            } catch (Exception ignored) {
                // fallback to next factory
            }
        }
        return executor != null ? new JdkHttpClient(executor) : new JdkHttpClient();
    }

    @Nonnull
    public static CompletionStage<? extends Response> execute(@Nonnull Request request) {
        return DEFAULT_CLIENT_FACTORY.get().execute(request);
    }

    @Nonnull
    @SuppressWarnings("resource")
    public static CompletionStage<? extends Response> newExecute(@Nonnull Request request) {
        final HttpClient client = defaultClient();
        return client.execute(request);
    }
}
