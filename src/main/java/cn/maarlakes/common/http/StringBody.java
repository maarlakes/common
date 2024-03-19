package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.*;

/**
 * @author linjpxc
 */
public class StringBody implements Request.Body {

    private final byte[] content;
    private final ContentType contentType;
    private final Header contentEncoding;

    public StringBody(@Nonnull String value, @Nonnull ContentType contentType) {
        this(value, contentType, null);
    }

    public StringBody(@Nonnull String value, @Nonnull ContentType contentType, Header contentEncoding) {
        try {
            this.content = contentType.getCharset() == null ? value.getBytes() : value.getBytes(contentType.getCharset());
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;
    }

    @Override
    public int getContentLength() {
        return this.content.length;
    }

    @Nonnull
    @Override
    public ContentType getContentType() {
        return this.contentType;
    }

    @Override
    public Header getContentEncoding() {
        return this.contentEncoding;
    }

    @Override
    public InputStream getContent() {
        return new ByteArrayInputStream(this.content);
    }

    @Override
    public void writeTo(@Nonnull OutputStream stream) {
        try {
            stream.write(this.content);
            stream.flush();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
