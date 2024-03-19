package cn.maarlakes.common.http.async;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import cn.maarlakes.common.utils.ClassUtils;

/**
 * @author linjpxc
 */
@Order
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class AsyncHttpClientFactory implements HttpClientFactory {

    private static final boolean OK = ClassUtils.hasClass("org.asynchttpclient.AsyncHttpClient");

    @Override
    public HttpClient createClient() {
        if (OK) {
            return new NettyAsyncHttpClient();
        }
        return null;
    }
}
