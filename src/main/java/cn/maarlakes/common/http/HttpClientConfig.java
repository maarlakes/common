package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.net.ssl.SSLContext;
import java.util.concurrent.Executor;

public interface HttpClientConfig extends RequestConfig {

    @Nullable
    SSLContext getSslContext();

    Executor getExecutor();

    @Nonnull
    static Builder builder() {
        return new HttpClientConfigBuilder();
    }

    interface Builder extends BaseRequestConfigBuilder<Builder, HttpClientConfig> {

        @Nonnull
        Builder sslContext(@Nullable SSLContext sslContext);

        @Nonnull
        Builder executor(@Nullable Executor executor);
    }
}
