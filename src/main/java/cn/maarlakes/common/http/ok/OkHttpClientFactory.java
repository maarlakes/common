package cn.maarlakes.common.http.ok;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import cn.maarlakes.common.utils.ClassUtils;

/**
 * @author linjpxc
 */
@Order(20)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class OkHttpClientFactory implements HttpClientFactory {

    private static final boolean OK = ClassUtils.hasClass("okhttp3.OkHttpClient");

    @Override
    public HttpClient createClient() {
        if (OK) {
            return new OkAsyncHttpClient();
        }
        return null;
    }
}
