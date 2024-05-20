package cn.maarlakes.common.http.async;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.function.Function0;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import cn.maarlakes.common.utils.ClassUtils;
import io.netty.channel.nio.NioEventLoopGroup;
import jakarta.annotation.Nonnull;
import org.asynchttpclient.DefaultAsyncHttpClientConfig;
import org.asynchttpclient.Dsl;

import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;

/**
 * @author linjpxc
 */
@Order(Integer.MAX_VALUE)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class AsyncHttpClientFactory implements HttpClientFactory {

    private static final boolean OK = ClassUtils.hasClass("org.asynchttpclient.AsyncHttpClient");

    @Nonnull
    @Override
    public HttpClient createClient() {
        return new NettyAsyncHttpClient();
    }

    @Nonnull
    @Override
    public HttpClient createClient(@Nonnull Function0<Executor> executorFactory) {
        final DefaultAsyncHttpClientConfig.Builder builder = new DefaultAsyncHttpClientConfig.Builder();
        builder.setEventLoopGroup(new NioEventLoopGroup(0, executorFactory.get(), SelectorProvider.provider()));
        return new NettyAsyncHttpClient(Dsl.asyncHttpClient(builder));
    }

    @Override
    public boolean isAvailable() {
        return OK;
    }
}
