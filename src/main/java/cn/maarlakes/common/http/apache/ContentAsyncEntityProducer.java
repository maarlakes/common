package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.ContentTypes;
import cn.maarlakes.common.http.body.ContentBody;
import cn.maarlakes.common.http.body.ContentChannel;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于 {@link ContentBody} 的 Apache HttpClient 5 异步实体生产者。
 *
 * <p>将统一的 {@link ContentBody} 适配为 Apache 5 的 {@link AsyncEntityProducer} 接口。
 * 优先使用 {@link ContentChannel} 的通道传输模式以提高性能；
 * 若不支持通道模式则回退到回调式写入。内容长度返回 -1（未知），
 * 由 Apache 框架自动处理分块传输。</p>
 *
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
        if (this.body instanceof ContentChannel) {
            final ContentChannel contentChannel = (ContentChannel) this.body;
            contentChannel.transferTo(new WritableByteChannel() {
                @Override
                public int write(ByteBuffer src) throws IOException {
                    return channel.write(src);
                }

                @Override
                public boolean isOpen() {
                    return true;
                }

                @Override
                public void close() throws IOException {
                }
            });
            channel.endStream();
        } else {
            this.body.writeTo((buffer, offset, length) -> {
                final ByteBuffer buf = ByteBuffer.wrap(buffer, offset, length);
                channel.write(buf);
                buf.clear();
            });
            channel.endStream();
        }
    }

    @Override
    public void releaseResources() {
    }
}
