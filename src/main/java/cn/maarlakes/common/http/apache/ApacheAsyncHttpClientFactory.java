package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientConfig;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;

/**
 * 基于 Apache HttpClient 5（异步）的 {@link HttpClientFactory} SPI 实现。
 *
 * <p>优先级 {@code @Order(200)}，在 OkHttp（100）之后、Apache HC4（300）之前。
 * 使用 Apache HttpAsyncClient 的非阻塞 API 发送请求。
 *
 * <p>需要 {@code org.apache.hc.client5:httpclient5} 在 classpath 上，
 * 否则 {@link #createClient} 会抛出异常，SPI 加载器会跳过此工厂。
 *
 * @author linjpxc
 */
@Order(200)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class ApacheAsyncHttpClientFactory implements HttpClientFactory {

    private static final Logger log = LoggerFactory.getLogger(ApacheAsyncHttpClientFactory.class);

    @Nonnull
    @Override
    public HttpClient createClient(@Nonnull HttpClientConfig config) {
        SSLContext ssl = config.getSslContext();
        final HttpAsyncClient client = ssl != null
                ? ApacheAsyncHttpClient.buildClient(ssl)
                : HttpAsyncClientBuilder.create().build();
        log.info("已创建 Apache AsyncHttpClient (HC5), SSL 已启用: {}", ssl != null);
        return new ApacheAsyncHttpClient(client, config);
    }
}
