package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.function.Consumer3;
import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.body.AbstractByteArrayBody;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.nio.charset.Charset;

/**
 * @author linjpxc
 */
public abstract class AbstractByteArrayPart<T> extends AbstractPart<T> {
    private volatile AbstractByteArrayBody<byte[]> body;
    private final Object bodyLock = new Object();

    public AbstractByteArrayPart(@Nonnull String name, ContentType contentType, Charset charset) {
        super(name, contentType, charset);
    }

    @Override
    public InputStream getContentStream() {
        return this.getBody().getContentStream();
    }

    @Override
    public int getContentLength() {
        return this.getBody().getContentLength();
    }

    @Override
    public void writeTo(@Nonnull Consumer3<byte[], Integer, Integer> consumer) {
        this.getBody().writeTo(consumer);
    }

    protected abstract AbstractByteArrayBody<byte[]> createContentBody();

    private AbstractByteArrayBody<byte[]> getBody() {
        if (body == null) {
            synchronized (bodyLock) {
                if (body == null) {
                    body = createContentBody();
                }
            }
        }
        return body;
    }
}
