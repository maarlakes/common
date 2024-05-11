package cn.maarlakes.common.http;

import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.Lazy;
import cn.maarlakes.common.utils.NamedThreadFactory;
import jakarta.annotation.Nonnull;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * @author linjpxc
 */
public final class HttpClients {
    private HttpClients() {
    }

    private static final AtomicInteger ID = new AtomicInteger(0);

    private static final Supplier<HttpClient> DEFAULT_CLIENT_FACTORY = Lazy.of(HttpClients::defaultClient);
    private static final HttpClientFactory HTTP_CLIENT_FACTORY;

    static {
        HTTP_CLIENT_FACTORY = SpiServiceLoader.loadShared(HttpClientFactory.class, HttpClientFactory.class.getClassLoader())
                .stream().filter(HttpClientFactory::isAvailable).findFirst().orElse(null);
    }

    @Nonnull
    public static HttpClient defaultClient() {
        return HTTP_CLIENT_FACTORY == null ? new JdkHttpClient() : HTTP_CLIENT_FACTORY.createClient();
    }

    @Nonnull
    public static HttpClient createClient() {
        return createClient(new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 4, 1, TimeUnit.MINUTES, new SynchronousQueue<>(), new NamedThreadFactory("http-client-" + ID.incrementAndGet() + "-")));
    }

    @Nonnull
    public static HttpClient createClient(@Nonnull Executor executor) {
        return HTTP_CLIENT_FACTORY == null ? new JdkHttpClient(executor) : HTTP_CLIENT_FACTORY.createClient(executor);
    }

    @Nonnull
    public static CompletionStage<? extends Response> execute(@Nonnull Request request) {
        return DEFAULT_CLIENT_FACTORY.get().execute(request);
    }

    @Nonnull
    @SuppressWarnings("resource")
    public static CompletionStage<? extends Response> newExecute(@Nonnull Request request) {
        final HttpClient client = defaultClient();
        return client.execute(request)
                .whenComplete((response, ex) -> {
                    try {
                        client.close();
                    } catch (Exception ignored) {
                    }
                });
    }
}
