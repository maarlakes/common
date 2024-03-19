package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface HttpClientFactory {

    @Nonnull
    HttpClient createClient();

    boolean isAvailable();
}
