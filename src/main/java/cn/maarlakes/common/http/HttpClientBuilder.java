package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author linjpxc
 */
public interface HttpClientBuilder {


    @Nonnull
    HttpClientBuilder factory(@Nonnull HttpClientFactory factory);

    @Nonnull
    HttpClientBuilder requestConfig(@Nonnull RequestConfig config);

    @Nonnull
    HttpClientBuilder addFilter(@Nonnull HttpFilter filter);

    HttpClientBuilder addFilter(@Nonnull List<HttpFilter> filters);

    @Nonnull
    HttpClientBuilder addFilter(@Nonnull HttpFilter... filters);

    @Nonnull
    HttpClient build();
}
