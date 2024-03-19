package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import cn.maarlakes.common.utils.ClassUtils;
import jakarta.annotation.Nonnull;

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

    @Override
    public boolean isAvailable() {
        return OK;
    }
}
