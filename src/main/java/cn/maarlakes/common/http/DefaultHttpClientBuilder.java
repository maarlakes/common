package cn.maarlakes.common.http;

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
            throw new IllegalStateException("factory is not set");
        }
        final HttpClientConfig config;
        if (this.requestConfig instanceof HttpClientConfig) {
            config = (HttpClientConfig) this.requestConfig;
        } else if (this.requestConfig != null) {
            config = HttpClientConfig.builder()
                    .redirectsEnabled(this.requestConfig.isRedirectsEnabled())
                    .requestTimeout(this.requestConfig.getRequestTimeout())
                    .connectTimeout(this.requestConfig.getConnectTimeout())
                    .responseTimeout(this.requestConfig.getResponseTimeout())
                    .proxy(this.requestConfig.getProxy())
                    .proxyAuthentication(this.requestConfig.getProxyAuthentication())
                    .maxRedirects(this.requestConfig.getMaxRedirects())
                    .build();
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
