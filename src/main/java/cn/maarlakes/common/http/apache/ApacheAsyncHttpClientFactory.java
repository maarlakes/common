package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import cn.maarlakes.common.utils.ClassUtils;
import jakarta.annotation.Nonnull;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;

/**
 * @author linjpxc
 */
@Order(10)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class ApacheAsyncHttpClientFactory implements HttpClientFactory {

    private static final boolean OK = ClassUtils.hasClass("org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient");

    @Nonnull
    @Override
    public HttpClient createClient() {
        return new ApacheAsyncHttpClient(HttpAsyncClientBuilder.create().build());
    }

    @Override
    public boolean isAvailable() {
        return OK;
    }
}
