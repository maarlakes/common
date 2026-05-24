package cn.maarlakes.common.http.body;

import jakarta.annotation.Nonnull;

import java.nio.channels.WritableByteChannel;

/**
 * 基于 NIO {@link WritableByteChannel} 的消息体传输通道接口。
 *
 * <p>与 {@link ContentBody} 基于流式 API 的写入方式互补，{@code ContentChannel}
 * 适用于底层 HTTP 客户端使用 NIO Channel 进行零拷贝传输的场景。</p>
 *
 * @author linjpxc
 */
public interface ContentChannel {

    /**
     * 将消息体内容传输到指定的 {@link WritableByteChannel}。
     *
     * @param channel 目标写入通道
     */
    void transferTo(@Nonnull WritableByteChannel channel);
}
