package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * HTTP 请求/响应过滤器，通过责任链模式实现请求拦截。
 *
 * <p>过滤器在 {@link FilterableHttpClient} 中组成责任链，按添加顺序依次执行。
 * 每个过滤器可以在请求发送前和响应返回后执行自定义逻辑，例如：
 * <ul>
 *   <li>请求/响应日志记录</li>
 *   <li>认证头注入</li>
 *   <li>请求重试</li>
 *   <li>指标采集</li>
 * </ul>
 *
 * <p>使用方式：实现 {@link #doFilter} 方法，在其中处理请求/响应，
 * 并通过 {@link Chain#doFilter} 将控制权传递给下一个过滤器。
 * 不调用 {@code chain.doFilter} 将中断责任链，请求不会发送。
 *
 * <p>标记为 {@link FunctionalInterface}，可以使用 Lambda 表达式。
 *
 * @author linjpxc
 */
@FunctionalInterface
public interface HttpFilter {

    /**
     * 执行过滤逻辑。
     *
     * @param request HTTP 请求
     * @param config  请求配置，可为 null
     * @param handler 响应处理器
     * @param chain   责任链，调用 {@code chain.doFilter} 传递给下一个过滤器
     * @param <T>     响应处理器的返回类型
     * @return 异步处理结果
     */
    @Nonnull
    <T> CompletableFuture<T> doFilter(@Nonnull Request request, RequestConfig config,
                                       @Nonnull ResponseHandler<T> handler, @Nonnull Chain chain);

    /**
     * 过滤器责任链接口，代表链中下一个节点的执行位置。
     *
     * <p>调用 {@link #doFilter} 将请求传递给下一个过滤器；
     * 如果已到达链尾，则执行实际的 HTTP 请求。
     */
    interface Chain {
        @Nonnull
        <T> CompletableFuture<T> doFilter(@Nonnull Request request, RequestConfig config,
                                           @Nonnull ResponseHandler<T> handler);
    }
}
