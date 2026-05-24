package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientConfig;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;

import javax.net.ssl.SSLContext;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * @author linjpxc
 */
@Order(300)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class ApacheHttpClient4Factory implements HttpClientFactory {

    @Nonnull
    @Override
    public HttpClient createClient(@Nonnull HttpClientConfig config) {
        final Executor executor = config.getExecutor();
        final SSLContext ssl = config.getSslContext();
        final org.apache.http.client.HttpClient client = ssl != null
                ? buildClient(ssl)
                : org.apache.http.impl.client.HttpClientBuilder.create().build();
        final boolean ownsExecutor = executor == null;
        return new ApacheHttpClient4(client, ownsExecutor ? new ForkJoinPool() : executor, ownsExecutor, config);
    }

    private static org.apache.http.client.HttpClient buildClient(SSLContext sslContext) {
        final org.apache.http.impl.client.HttpClientBuilder builder = org.apache.http.impl.client.HttpClientBuilder.create();
        builder.setSSLSocketFactory(new org.apache.http.conn.ssl.SSLConnectionSocketFactory(sslContext));
        return builder.build();
    }
}
