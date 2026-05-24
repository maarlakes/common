package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientConfig;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;

import javax.net.ssl.SSLContext;

/**
 * @author linjpxc
 */
@Order(200)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class ApacheAsyncHttpClientFactory implements HttpClientFactory {

    @Nonnull
    @Override
    public HttpClient createClient(@Nonnull HttpClientConfig config) {
        SSLContext ssl = config.getSslContext();
        final HttpAsyncClient client = ssl != null
                ? ApacheAsyncHttpClient.buildClient(ssl)
                : HttpAsyncClientBuilder.create().build();
        return new ApacheAsyncHttpClient(client, config);
    }
}
