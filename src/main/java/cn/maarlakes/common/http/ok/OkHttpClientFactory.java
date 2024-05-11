package cn.maarlakes.common.http.ok;

import cn.maarlakes.common.Order;
import cn.maarlakes.common.http.HttpClient;
import cn.maarlakes.common.http.HttpClientFactory;
import cn.maarlakes.common.spi.SpiService;
import cn.maarlakes.common.utils.ClassUtils;
import jakarta.annotation.Nonnull;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * @author linjpxc
 */
@Order(20)
@SpiService(lifecycle = SpiService.Lifecycle.SINGLETON)
public class OkHttpClientFactory implements HttpClientFactory {

    private static final boolean OK = ClassUtils.hasClass("okhttp3.OkHttpClient");

    @Nonnull
    @Override
    public HttpClient createClient() {
        return new OkAsyncHttpClient();
    }

    @Nonnull
    @Override
    public HttpClient createClient(@Nonnull Executor executor) {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        if (executor instanceof ExecutorService){
            builder.dispatcher(new Dispatcher((ExecutorService) executor));
        }
        return new OkAsyncHttpClient(builder.build());
    }

    @Override
    public boolean isAvailable() {
        return OK;
    }
}
