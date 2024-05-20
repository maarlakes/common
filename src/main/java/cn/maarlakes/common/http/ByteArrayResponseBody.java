package cn.maarlakes.common.http;

import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author linjpxc
 */
public class ByteArrayResponseBody implements ResponseBody {

    private final byte[] content;
    private final ContentType contentType;

    public ByteArrayResponseBody(@Nonnull byte[] content, ContentType contentType) {
        this.content = content;
        this.contentType = contentType;
    }

    @Nonnull
    @Override
    public InputStream getContent() {
        return new ByteArrayInputStream(this.content);
    }

    @Override
    public ContentType getContentType() {
        return this.contentType;
    }

    @Override
    public String asText(@Nonnull Charset charset) {
        return new String(this.content, charset);
    }

    @Override
    public byte[] asBytes() {
        return Arrays.copyOf(this.content, this.content.length);
    }

    @Override
    public <T> T toJsonObject(@Nonnull Class<T> type, @Nonnull Charset charset) {
        return JSON.parseObject(this.content, 0, this.content.length, charset, type);
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ByteArrayResponseBody)) {
            return false;
        }

        final ByteArrayResponseBody that = (ByteArrayResponseBody) o;
        return Arrays.equals(content, that.content) && Objects.equals(contentType, that.contentType);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(content);
        result = 31 * result + Objects.hashCode(contentType);
        return result;
    }
}
