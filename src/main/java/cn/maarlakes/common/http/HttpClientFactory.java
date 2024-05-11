package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.concurrent.Executor;

/**
 * @author linjpxc
 */
public interface HttpClientFactory {

    @Nonnull
    HttpClient createClient();

    @Nonnull
    HttpClient createClient(@Nonnull Executor executor);

    boolean isAvailable();
}
