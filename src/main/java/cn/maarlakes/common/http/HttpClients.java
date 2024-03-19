package cn.maarlakes.common.http;

import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.Lazy;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

/**
 * @author linjpxc
 */
public final class HttpClients {
    private HttpClients() {
    }

    private static final Supplier<HttpClient> DEFAULT_CLIENT_FACTORY = Lazy.of(HttpClients::defaultClient);

    @Nonnull
    public static HttpClient defaultClient() {
        for (HttpClientFactory factory : SpiServiceLoader.loadShared(HttpClientFactory.class, HttpClientFactory.class.getClassLoader())) {
            final HttpClient client = factory.createClient();
            if (client != null) {
                return client;
            }
        }
        return new JdkHttpClient();
    }

    @Nonnull
    public static CompletionStage<? extends Response> execute(@Nonnull Request request) {
        return DEFAULT_CLIENT_FACTORY.get().execute(request);
    }
}
