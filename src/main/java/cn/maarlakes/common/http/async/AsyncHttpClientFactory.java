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

import javax.net.ssl.SSLContext;
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;

/**
 * @author linjpxc
 */
@Order(400)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class AsyncHttpClientFactory implements HttpClientFactory {

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
            builder.setEventLoopGroup(new NioEventLoopGroup(0, executor, SelectorProvider.provider()));

        }
        return new NettyAsyncHttpClient(Dsl.asyncHttpClient(builder), config);
    }
}
