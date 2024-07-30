package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.ContentTypes;
import cn.maarlakes.common.http.body.ContentBody;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author linjpxc
 */
class ContentAsyncEntityProducer implements AsyncEntityProducer {
    private final ContentBody<?> body;
    private final AtomicReference<Exception> exception = new AtomicReference<>(null);

    ContentAsyncEntityProducer(ContentBody<?> body) {
        this.body = body;
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
        return -1;
    }

    @Override
    public String getContentType() {
        final ContentType contentType = this.body.getContentType();
        return contentType == null ? null : ContentTypes.toString(contentType);
    }

    @Override
    public String getContentEncoding() {
        return null;
    }

    @Override
    public boolean isChunked() {
        return false;
    }

    @Override
    public Set<String> getTrailerNames() {
        return null;
    }

    @Override
    public int available() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void produce(DataStreamChannel channel) throws IOException {
        this.body.writeTo((buffer, offset, length) -> {
            final ByteBuffer buf = ByteBuffer.wrap(buffer, offset, length);
            channel.write(buf);
            channel.endStream();
            buf.clear();
        });
    }

    @Override
    public void releaseResources() {
    }
}
