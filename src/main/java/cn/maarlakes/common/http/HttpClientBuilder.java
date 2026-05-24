package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * HTTP 客户端构建器，提供流式 API 配置和创建 {@link HttpClient} 实例。
 *
 * <p>通过 {@link HttpClient#builder()} 获取实例，支持以下配置：
 * <ul>
 *   <li>{@link #factory} — 指定 HTTP 客户端工厂（如 OkHttp、Apache），不指定则使用 JDK 内置实现</li>
 *   <li>{@link #requestConfig} — 客户端级默认配置（超时、SSL、代理等）</li>
 *   <li>{@link #addFilter} — 添加请求/响应过滤器，按添加顺序组成责任链</li>
 * </ul>
 *
 * <p>过滤器支持单个添加、列表添加和可变参数添加，可根据场景灵活使用。
 *
 * @author linjpxc
 */
public interface HttpClientBuilder {

    /**
     * 指定 HTTP 客户端工厂。不指定时，{@link #build()} 创建 JDK 内置实现。
     */
    @Nonnull
    HttpClientBuilder factory(@Nonnull HttpClientFactory factory);

    /**
     * 设置客户端级默认请求配置。
     */
    @Nonnull
    HttpClientBuilder requestConfig(@Nonnull RequestConfig config);

    /**
     * 添加单个过滤器到责任链末尾。
     */
    @Nonnull
    HttpClientBuilder addFilter(@Nonnull HttpFilter filter);

    /**
     * 添加过滤器列表到责任链末尾。
     */
    HttpClientBuilder addFilter(@Nonnull List<HttpFilter> filters);

    /**
     * 添加可变参数过滤器到责任链末尾。
     */
    @Nonnull
    HttpClientBuilder addFilter(@Nonnull HttpFilter... filters);

    /**
     * 构建并返回配置好的 {@link HttpClient} 实例。
     *
     * <p>如果有注册过滤器，返回的是被 {@link FilterableHttpClient} 装饰的实例。
     */
    @Nonnull
    HttpClient build();
}
