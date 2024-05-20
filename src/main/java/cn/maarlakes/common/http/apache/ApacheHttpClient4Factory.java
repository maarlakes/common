package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.function.Function0;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import cn.maarlakes.common.utils.ClassUtils;
import jakarta.annotation.Nonnull;

import java.util.concurrent.Executor;

/**
 * @author linjpxc
 */
@Order(100)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class ApacheHttpClient4Factory implements HttpClientFactory {

    private static final boolean OK = ClassUtils.hasClass("org.apache.http.impl.client.CloseableHttpClient");

    @Nonnull
    @Override
    public HttpClient createClient() {
        return new ApacheHttpClient4();
    }

    @Nonnull
    @Override
    public HttpClient createClient(@Nonnull Function0<Executor> executorFactory) {
        return new ApacheHttpClient4(executorFactory.get());
    }

    @Override
    public boolean isAvailable() {
        return OK;
    }
}
