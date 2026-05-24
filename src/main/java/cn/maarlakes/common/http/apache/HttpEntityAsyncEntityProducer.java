package cn.maarlakes.common.http.apache;

import jakarta.annotation.Nonnull;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于 Apache HttpClient 5 {@link HttpEntity} 的异步实体生产器适配器。
 *
 * <p>将阻塞式的 {@link HttpEntity} 适配为异步的 {@link AsyncEntityProducer}。
 * 通过 {@link ByteArrayOutputStream} 将实体内容一次性读入内存后写入通道，
 * 适用于实体内容较小且不支持流式传输的场景。</p>
 *
 * @author linjpxc
 */
class HttpEntityAsyncEntityProducer implements AsyncEntityProducer {

    private final HttpEntity entity;
    private final AtomicReference<Exception> exception = new AtomicReference<>(null);

    public HttpEntityAsyncEntityProducer(@Nonnull HttpEntity entity) {
        this.entity = entity;
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public void failed(Exception cause) {
        if (exception.compareAndSet(null, cause)) {
            releaseResources();
        }
    }

    @Override
    public long getContentLength() {
        return this.entity.getContentLength();
    }

    @Override
    public String getContentType() {
        return this.entity.getContentType();
    }

    @Override
    public String getContentEncoding() {
        return this.entity.getContentEncoding();
    }

    @Override
    public boolean isChunked() {
        return this.entity.isChunked();
    }

    @Override
    public Set<String> getTrailerNames() {
        return this.entity.getTrailerNames();
    }

    @Override
    public int available() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void produce(DataStreamChannel channel) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            this.entity.writeTo(out);
            final ByteBuffer buffer = ByteBuffer.wrap(out.toByteArray());
            channel.write(buffer);
            channel.endStream();
            buffer.clear();
        }
    }

    @Override
    public void releaseResources() {
        try {
            this.entity.close();
        } catch (IOException ignored) {
        }
    }
}
