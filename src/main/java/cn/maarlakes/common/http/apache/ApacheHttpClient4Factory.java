package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientConfig;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * 基于 Apache HttpClient 4（经典/同步）的 {@link HttpClientFactory} SPI 实现。
 *
 * <p>优先级 {@code @Order(300)}，在 Apache HC5（200）之后、AsyncHttpClient（400）之前。
 * 使用 Apache HttpClient 4 的经典阻塞 API，内部通过线程池转为异步。
 *
 * <p>需要 {@code org.apache.httpcomponents:httpclient} 在 classpath 上，
 * 否则 {@link #createClient} 会抛出异常，SPI 加载器会跳过此工厂。
 *
 * @author linjpxc
 */
@Order(300)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class ApacheHttpClient4Factory implements HttpClientFactory {

    private static final Logger log = LoggerFactory.getLogger(ApacheHttpClient4Factory.class);

    @Nonnull
    @Override
    public HttpClient createClient(@Nonnull HttpClientConfig config) {
        final Executor executor = config.getExecutor();
        final SSLContext ssl = config.getSslContext();
        final org.apache.http.client.HttpClient client = ssl != null
                ? buildClient(ssl)
                : org.apache.http.impl.client.HttpClientBuilder.create().build();
        // 未配置执行器时创建默认的 ForkJoinPool 并标记为 owned，close 时自动关闭
        final boolean ownsExecutor = executor == null;
        log.info("已创建 Apache HttpClient 4 (经典), SSL 已启用: {}, 自有执行器: {}", ssl != null, ownsExecutor);
        return new ApacheHttpClient4(client, ownsExecutor ? new ForkJoinPool() : executor, ownsExecutor, config);
    }

    private static org.apache.http.client.HttpClient buildClient(SSLContext sslContext) {
        final org.apache.http.impl.client.HttpClientBuilder builder = org.apache.http.impl.client.HttpClientBuilder.create();
        builder.setSSLSocketFactory(new org.apache.http.conn.ssl.SSLConnectionSocketFactory(sslContext));
        return builder.build();
    }
}
