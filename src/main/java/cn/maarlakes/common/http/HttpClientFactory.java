package cn.maarlakes.common.http;

import cn.maarlakes.common.function.Function0;
import jakarta.annotation.Nonnull;

import java.util.concurrent.Executor;

/**
 * @author linjpxc
 */
public interface HttpClientFactory {

    @Nonnull
    HttpClient createClient();

    @Nonnull
    default HttpClient createClient(@Nonnull Executor executor) {
        return this.createClient(() -> executor);
    }

    @Nonnull
    HttpClient createClient(@Nonnull Function0<Executor> executorFactory);

    boolean isAvailable();
}
