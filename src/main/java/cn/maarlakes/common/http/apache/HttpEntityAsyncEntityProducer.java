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
            buffer.clear();
        }
    }

    @Override
    public void releaseResources() {
        try {
            this.entity.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
