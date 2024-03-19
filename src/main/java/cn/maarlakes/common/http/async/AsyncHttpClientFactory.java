package cn.maarlakes.common.http.async;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import cn.maarlakes.common.utils.ClassUtils;
import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
@Order
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class AsyncHttpClientFactory implements HttpClientFactory {

    private static final boolean OK = ClassUtils.hasClass("org.asynchttpclient.AsyncHttpClient");

    @Nonnull
    @Override
    public HttpClient createClient() {
        return new NettyAsyncHttpClient();
    }

    @Override
    public boolean isAvailable() {
        return OK;
    }
}
