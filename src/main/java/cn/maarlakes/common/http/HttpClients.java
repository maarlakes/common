package cn.maarlakes.common.http;

import cn.maarlakes.common.http.jdk.JdkHttpClient;
import cn.maarlakes.common.spi.SpiServiceLoader;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

/**
 * HTTP 客户端静态工具类，提供无状态的快捷请求方法。
 *
 * <p>内部通过 SPI 自动发现可用的 {@link HttpClientFactory}，按 {@code @Order} 优先级依次尝试创建客户端。
 * 所有可选 HTTP 库（Apache、OkHttp、AsyncHttpClient）均为 {@code <optional>} 依赖，
 * 当 classpath 上缺少某个库时，会触发 {@link NoClassDefFoundError}，此时自动排除该工厂并重试下一个。
 * 若所有 SPI 工厂均不可用，最终回退到基于 JDK {@link java.net.HttpURLConnection} 的内置实现。
 *
 * <p>线程安全：使用 volatile + synchronized 的双重检查锁定模式保证单例客户端的延迟初始化。
 * 一旦创建成功，客户端实例被全局共享，{@code close()} 不应被外部调用。
 *
 * @author linjpxc
 */
public final class HttpClients {

    private static final Logger log = LoggerFactory.getLogger(HttpClients.class);

    /**
     * 全局共享的 HTTP 客户端实例。volatile 保证多线程下的可见性。
     */
    private static volatile HttpClient CLIENT;

    /**
     * 创建当前客户端的工厂引用，用于在 NoClassDefFoundError 时定位并排除问题工厂。
     */
    private static HttpClientFactory FACTORY;

    /**
     * 已确认不可用的工厂类集合，避免重复尝试。由 {@code synchronized(LOCK)} 保护。
     */
    private static final Set<Class<?>> EXCLUDES = new HashSet<>();

    /** 全局锁，保护 CLIENT/FACTORY/EXCLUDES 的并发访问。 */
    private static final Object LOCK = new Object();

    private HttpClients() {
    }

    /**
     * 使用默认配置发送请求。
     */
    @Nonnull
    public static CompletableFuture<Response> execute(@Nonnull Request request) {
        return doExecute(client -> client.execute(request));
    }

    /**
     * 使用指定配置发送请求。
     */
    @Nonnull
    public static CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config) {
        return doExecute(client -> client.execute(request, config));
    }

    /**
     * 使用指定配置发送请求，并通过 handler 流式处理响应体。
     */
    @Nonnull
    public static <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler) {
        return doExecute(client -> client.execute(request, config, handler));
    }

    /**
     * 使用默认配置发送请求，并通过 handler 流式处理响应体。
     */
    @Nonnull
    public static <T> CompletableFuture<T> execute(@Nonnull Request request, @Nonnull ResponseHandler<T> handler) {
        return doExecute(client -> client.execute(request, null, handler));
    }

    /**
     * 统一的执行入口，封装 SPI 工厂的 fallback 逻辑。
     *
     * <p>执行流程：
     * <ol>
     *   <li>尝试使用当前缓存的客户端执行请求</li>
     *   <li>若抛出 {@link NoClassDefFoundError}（可选依赖不在 classpath），排除当前工厂并重置缓存</li>
     *   <li>重新获取客户端（会尝试下一个 SPI 工厂），再次执行</li>
     *   <li>若仍然失败，返回异常完成的 Future，不向外抛出 Error</li>
     * </ol>
     *
     * <p>注意：{@link NoClassDefFoundError} 是 JVM 级别的 Error，不是 Exception，
     * 只有在首次使用缺失类的某个方法时才会触发，因此需要在运行时捕获。
     */
    private static <T> CompletableFuture<T> doExecute(HttpClientExecutor<T> executor) {
        try {
            return executor.execute(getDefault());
        } catch (NoClassDefFoundError e) {
            // 可选依赖缺失，需要排除当前工厂并重新初始化
            log.warn("使用 {} 时发生 NoClassDefFoundError: {}, 回退到下一个可用工厂",
                    FACTORY != null ? FACTORY.getClass().getName() : "unknown", e.getMessage());
            synchronized (LOCK) {
                final HttpClientFactory factory = FACTORY;
                if (factory != null) {
                    EXCLUDES.add(factory.getClass());
                    log.debug("已排除工厂: {}", factory.getClass().getName());
                }
                CLIENT = null;
                FACTORY = null;
            }
            try {
                return executor.execute(getDefault());
            } catch (NoClassDefFoundError ex) {
                log.error("所有 HTTP 客户端工厂均不可用, 返回异常 Future", ex);
                final CompletableFuture<T> future = new CompletableFuture<>();
                future.completeExceptionally(ex);
                return future;
            }
        }
    }

    /**
     * 获取或创建全局默认的 HTTP 客户端。
     *
     * <p>使用 {@code synchronized(LOCK)} 保证只会创建一个实例。
     * 遍历 SPI 加载的 {@link HttpClientFactory}，按优先级尝试创建：
     * <ul>
     *   <li>已排除的工厂直接跳过</li>
     *   <li>创建成功的工厂缓存为全局客户端</li>
     *   <li>创建失败的工厂加入排除列表</li>
     * </ul>
     * 若所有工厂均失败，回退到 {@link JdkHttpClient}。
     */
    @Nonnull
    private static HttpClient getDefault() {
        synchronized (LOCK) {
            if (CLIENT == null) {
                try {
                    final HttpClientConfig config = HttpClientConfig.builder()
                            .executor(new ForkJoinPool())
                            .build();
                    for (HttpClientFactory factory : SpiServiceLoader.loadShared(HttpClientFactory.class)) {
                        if (EXCLUDES.contains(factory.getClass())) {
                            log.trace("跳过已排除的工厂: {}", factory.getClass().getName());
                            continue;
                        }
                        try {
                            CLIENT = factory.createClient(config);
                            FACTORY = factory;
                            log.info("通过工厂初始化默认 HTTP 客户端: {}", factory.getClass().getName());
                            break;
                        } catch (Exception e) {
                            EXCLUDES.add(factory.getClass());
                            log.debug("工厂 {} 创建客户端失败: {}", factory.getClass().getName(), e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    log.warn("加载 SPI 工厂失败, 将回退到 JdkHttpClient", e);
                }
                if (CLIENT == null) {
                    CLIENT = new JdkHttpClient(new ForkJoinPool());
                    FACTORY = null;
                    log.info("无可用 SPI 工厂, 使用内置 JdkHttpClient 作为回退");
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
