package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

/**
 * HTTP 客户端工厂的 SPI 接口。
 *
 * <p>每个具体的 HTTP 客户端实现（Apache、OkHttp 等）提供一个 Factory 实现，
 * 并在 {@code META-INF/services/cn.maarlakes.common.http.HttpClientFactory} 中声明。
 * 框架通过 {@link cn.maarlakes.common.spi.SpiServiceLoader} 自动发现并加载。
 *
 * <p>实现类应使用 {@code @SpiService(lifecycle = SINGLETON)} 标注以保证单例语义，
 * 并通过 {@code @Order} 控制优先级（数值越小优先级越高）。
 *
 * @author linjpxc
 */
public interface HttpClientFactory {

    /**
     * 根据客户端配置创建 HTTP 客户端实例。
     *
     * <p>如果所需底层库不在 classpath 上，应抛出异常而非返回 null，
     * 以便上层 SPI 加载器跳过此工厂并尝试下一个。
     *
     * @param config 客户端级配置（执行器、SSL 上下文、代理等），不允许为 null
     * @return 新的 HTTP 客户端实例
     */
    @Nonnull
    HttpClient createClient(@Nonnull HttpClientConfig config);
}
