package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Closeable;
import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public interface HttpClient extends Closeable {

    @Nonnull
    CompletionStage<? extends Response> execute(@Nonnull Request request);
}
