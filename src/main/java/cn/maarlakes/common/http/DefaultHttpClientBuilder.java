package cn.maarlakes.common.http;

import cn.maarlakes.common.http.jdk.JdkHttpClient;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * {@link HttpClientBuilder} 的默认实现，构建配置好的 {@link HttpClient} 实例。
 *
 * <p>构建流程：
 * <ol>
 *   <li>如果指定了 {@link HttpClientFactory}，通过工厂创建客户端，适用于 SPI 发现的第三方实现</li>
 *   <li>如果未指定工厂，创建基于 JDK HttpURLConnection 的 {@link JdkHttpClient}</li>
 *   <li>如果注册了 {@link HttpFilter}，用 {@link FilterableHttpClient} 装饰底层客户端</li>
 * </ol>
 *
 * <p>配置转换策略：当通过 {@link #requestConfig(RequestConfig)} 传入的配置是
 * {@link HttpClientConfig} 实例时直接使用；否则从 {@link RequestConfig} 转换为
 * {@link HttpClientConfig}，因为工厂接口需要后者。
 *
 * @author linjpxc
 */
class DefaultHttpClientBuilder implements HttpClientBuilder {

    private static final Logger log = LoggerFactory.getLogger(DefaultHttpClientBuilder.class);

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
        final HttpClient client;
        if (this.factory == null) {
            // 未指定工厂，使用 JDK 内置实现
            client = new JdkHttpClient(this.requestConfig);
            log.debug("已构建 JdkHttpClient (未指定工厂)");
        } else {
            // 将 RequestConfig 转换为 HttpClientConfig（工厂接口需要后者）
            final HttpClientConfig config;
            if (this.requestConfig instanceof HttpClientConfig) {
                config = (HttpClientConfig) this.requestConfig;
            } else if (this.requestConfig != null) {
                config = HttpClientConfig.builder().from(this.requestConfig).build();
            } else {
                config = HttpClientConfig.builder().build();
            }
            client = this.factory.createClient(config);
            log.debug("通过工厂构建 HttpClient: {}", this.factory.getClass().getName());
        }

        // 注册了过滤器时，用装饰器包装
        if (!this.filters.isEmpty()) {
            log.debug("使用 {} 个过滤器包装客户端", this.filters.size());
            return new FilterableHttpClient(client, this.filters);
        }
        return client;
    }
}
