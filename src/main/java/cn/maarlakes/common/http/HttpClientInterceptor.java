package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface HttpClientInterceptor {

    @Nonnull
    CompletionStage<? extends Response> intercept(@Nonnull Request request, RequestConfig config, @Nonnull Chain chain);

    interface Chain {
        @Nonnull
        CompletionStage<? extends Response> proceed(@Nonnull Request request, RequestConfig config);
    }
}
