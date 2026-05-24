package cn.maarlakes.common.http.async;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientConfig;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import io.netty.channel.nio.NioEventLoopGroup;
import jakarta.annotation.Nonnull;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.netty.ssl.JsseSslEngineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;

/**
 * 基于 AsyncHttpClient（Netty）的 {@link HttpClientFactory} SPI 实现。
 *
 * <p>优先级 {@code @Order(400)}，在所有 SPI 工厂中最低，作为最后的 Netty 方案。
 * 使用 AsyncHttpClient 的 Netty 后端进行异步 HTTP 通信。
 *
 * <p>需要 {@code org.asynchttpclient:async-http-client} 和 {@code io.netty:netty} 在 classpath 上，
 * 否则 {@link #createClient} 会抛出异常，SPI 加载器会跳过此工厂。
 *
 * @author linjpxc
 */
@Order(400)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class AsyncHttpClientFactory implements HttpClientFactory {

    private static final Logger log = LoggerFactory.getLogger(AsyncHttpClientFactory.class);

    @Nonnull
    @Override
    public HttpClient createClient(@Nonnull HttpClientConfig config) {
        final SSLContext ssl = config.getSslContext();
        final Executor executor = config.getExecutor();
        final DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();

        if (ssl != null) {
            builder.setSslEngineFactory(new JsseSslEngineFactory(ssl));
        }
        if (executor != null) {
            // 将通用 Executor 包装为 Netty 的 NioEventLoopGroup
            builder.setEventLoopGroup(new NioEventLoopGroup(0, executor, SelectorProvider.provider()));
        }
        log.info("已创建 Netty AsyncHttpClient, SSL 已启用: {}, 自定义执行器: {}",
                ssl != null, executor != null);
        return new NettyAsyncHttpClient(Dsl.asyncHttpClient(builder), config);
    }
}
