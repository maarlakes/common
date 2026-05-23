package cn.maarlakes.common.http;

import cn.maarlakes.common.http.jdk.JdkHttpClient;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author linjpxc
 */
class DefaultHttpClientBuilder implements HttpClientBuilder {

    private HttpClientFactory factory;
    private RequestConfig requestConfig;
    private final List<HttpFilter> filters = new ArrayList<>();

    @Nonnull
    @Override
    public HttpClientBuilder factory(@Nonnull HttpClientFactory factory) {
        this.factory = factory;
        return this;
    }

    @Nonnull
    @Override
    public HttpClientBuilder requestConfig(@Nonnull RequestConfig config) {
        this.requestConfig = config;
        return this;
    }

    @Nonnull
    @Override
    public HttpClientBuilder addFilter(@Nonnull HttpFilter filter) {
        this.filters.add(filter);
        return this;
    }

    @Override
    public HttpClientBuilder addFilter(@Nonnull List<HttpFilter> filters) {
        this.filters.addAll(filters);
        return this;
    }

    @Nonnull
    @Override
    public HttpClientBuilder addFilter(@Nonnull HttpFilter... filters) {
        this.filters.addAll(Arrays.asList(filters));
        return this;
    }

    @Nonnull
    @Override
    public HttpClient build() {
        if (this.factory == null) {
            HttpClient client = new JdkHttpClient(this.requestConfig);
            if (!this.filters.isEmpty()) {
                client = new FilterableHttpClient(client, this.filters);
            }
            return client;
        }
        final HttpClientConfig config;
        if (this.requestConfig instanceof HttpClientConfig) {
            config = (HttpClientConfig) this.requestConfig;
        } else if (this.requestConfig != null) {
            config = HttpClientConfig.builder().from(this.requestConfig).build();
        } else {
            config = HttpClientConfig.builder().build();
        }
        HttpClient client = this.factory.createClient(config);
        if (!this.filters.isEmpty()) {
            client = new FilterableHttpClient(client, this.filters);
        }
        return client;
    }
}
