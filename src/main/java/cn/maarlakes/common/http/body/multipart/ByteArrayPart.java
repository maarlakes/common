package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.function.Consumer3;
import cn.maarlakes.common.http.ContentType;
import jakarta.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author linjpxc
 */
public class ByteArrayPart extends AbstractPart<byte[]> {
    private final byte[] value;

    public ByteArrayPart(@Nonnull String name, @Nonnull byte[] value) {
        this(name, value, null, StandardCharsets.UTF_8);
    }

    public ByteArrayPart(@Nonnull String name, @Nonnull byte[] value, Charset charset) {
        this(name, value, null, charset);
    }

    public ByteArrayPart(@Nonnull String name, @Nonnull byte[] value, ContentType contentType) {
        this(name, value, contentType, StandardCharsets.UTF_8);
    }

    public ByteArrayPart(@Nonnull String name, @Nonnull byte[] value, ContentType contentType, Charset charset) {
        super(name, contentType, charset);
        this.value = value;
    }

    @Override
    public InputStream getContentStream() {
        return new ByteArrayInputStream(this.value);
    }

    @Override
    public byte[] getContent() {
        return this.value;
    }

    @Override
    public int getContentLength() {
        return this.value.length;
    }

    @Override
    public void writeTo(@Nonnull Consumer3<byte[], Integer, Integer> consumer) {
        consumer.acceptUnchecked(this.value, 0, this.value.length);
    }
}
