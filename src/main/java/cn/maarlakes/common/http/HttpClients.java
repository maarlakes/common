//package cn.maarlakes.common.http;
//
//import cn.maarlakes.common.http.jdk.JdkHttpClient;
//import cn.maarlakes.common.spi.SpiServiceLoader;
//import cn.maarlakes.common.utils.Lazy;
//import cn.maarlakes.common.utils.NamedThreadFactory;
//import jakarta.annotation.Nonnull;
//
//import java.util.List;
//import java.util.concurrent.*;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.function.Supplier;
//import java.util.stream.Collectors;
//
///**
// * @author linjpxc
// */
//public final class HttpClients {
//    private HttpClients() {
//    }
//
//    private static final AtomicInteger ID = new AtomicInteger(0);
//
//    private static final Supplier<HttpClient> DEFAULT_CLIENT_FACTORY = Lazy.of(HttpClients::defaultClient);
//    private static final List<HttpClientFactory> HTTP_CLIENT_FACTORIES;
//
//    static {
//        HTTP_CLIENT_FACTORIES = SpiServiceLoader.loadShared(HttpClientFactory.class, HttpClientFactory.class.getClassLoader())
//                .stream().filter(item -> {
//                    try {
//                        return item.isAvailable();
//                    } catch (NoClassDefFoundError ignored) {
//                        return false;
//                    }
//                }).collect(Collectors.toList());
//    }
//
//    @Nonnull
//    public static HttpClient defaultClient() {
//        return createClientWithFallback(null);
//    }
//
//    @Nonnull
//    public static HttpClient createClient() {
//        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
//                Runtime.getRuntime().availableProcessors(),
//                Runtime.getRuntime().availableProcessors() * 4,
//                1, TimeUnit.MINUTES,
//                new LinkedBlockingQueue<Runnable>(),
//                new NamedThreadFactory("maarlakes-http-client-" + ID.incrementAndGet() + "-")
//        );
//        executor.allowCoreThreadTimeOut(true);
//        return createClientWithFallback(executor);
//    }
//
//    @Nonnull
//    public static HttpClient createClient(@Nonnull Executor executor) {
//        return createClientWithFallback(executor);
//    }
//
//    @Nonnull
//    public static HttpClient createClient(@Nonnull HttpFilter... interceptors) {
//        return createClient((Executor) null, interceptors);
//    }
//
//    @Nonnull
//    public static HttpClient createClient(Executor executor, @Nonnull HttpFilter... interceptors) {
//        final HttpClient client = createClientWithFallback(executor);
//        if (interceptors == null || interceptors.length == 0) {
//            return client;
//        }
//        return new InterceptableHttpClient(client, java.util.Arrays.asList(interceptors));
//    }
//
//    @Nonnull
//    private static HttpClient createClientWithFallback(Executor executor) {
//        for (HttpClientFactory factory : HTTP_CLIENT_FACTORIES) {
//            try {
//                if (executor != null) {
//                    return factory.createClient(executor);
//                }
//                return factory.createClient();
//            } catch (Exception ignored) {
//                // fallback to next factory
//            }
//        }
//        return executor != null ? new JdkHttpClient(executor) : new JdkHttpClient();
//    }
//
//    @Nonnull
//    public static CompletableFuture<Response> execute(@Nonnull Request request) {
//        return DEFAULT_CLIENT_FACTORY.get().execute(request);
//    }
//
//    @Nonnull
//    public static HttpClient createRetryClient(int maxRetries) {
//        return new RetryHttpClient(defaultClient(), maxRetries);
//    }
//
//    @Nonnull
//    public static HttpClient createRetryClient(int maxRetries, long retryIntervalMillis) {
//        return new RetryHttpClient(defaultClient(), maxRetries, retryIntervalMillis, null);
//    }
//
//    @Nonnull
//    public static HttpClient createRetryClient(int maxRetries, long retryIntervalMillis, java.util.function.Predicate<Exception> retryPredicate) {
//        return new RetryHttpClient(defaultClient(), maxRetries, retryIntervalMillis, retryPredicate);
//    }
//}
