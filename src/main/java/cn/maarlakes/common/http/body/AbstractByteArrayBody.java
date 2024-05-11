package cn.maarlakes.common.http.body;

import cn.maarlakes.common.function.Consumer3;
import jakarta.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * @author linjpxc
 */
public abstract class  AbstractByteArrayBody<T> implements ContentBody<T> {

    private volatile byte[] buffer;
    private final Object bufferLock = new Object();

    @Override
    public InputStream getContentStream() {
        return new ByteArrayInputStream(this.getBuffer());
    }

    @Override
    public int getContentLength() {
        return this.getBuffer().length;
    }

    @Override
    public void writeTo(@Nonnull Consumer3<byte[], Integer, Integer> consumer) {
        final byte[] bytes = this.getBuffer();
        consumer.acceptUnchecked(bytes, 0, bytes.length);
    }

    protected final byte[] getBuffer() {
        if (this.buffer == null) {
            synchronized (this.bufferLock) {
                if (this.buffer == null) {
                    this.buffer = this.contentAsBytes();
                }
            }
        }
        return this.buffer;
    }

    protected abstract byte[] contentAsBytes();
}
