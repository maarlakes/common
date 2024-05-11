package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.body.AbstractByteArrayBody;
import cn.maarlakes.common.http.body.BodyUtils;
import jakarta.annotation.Nonnull;

import java.nio.charset.Charset;

/**
 * @author linjpxc
 */
public class DefaultTextPart extends AbstractByteArrayPart<CharSequence> implements TextPart {

    private final CharSequence value;

    public DefaultTextPart(@Nonnull String name, @Nonnull CharSequence value) {
        this(name, value, ContentType.TEXT_PLAIN, null);
    }

    public DefaultTextPart(@Nonnull String name, @Nonnull CharSequence value, Charset charset) {
        this(name, value, ContentType.TEXT_PLAIN, charset);
    }

    public DefaultTextPart(@Nonnull String name, @Nonnull CharSequence value, ContentType contentType) {
        this(name, value, contentType, toCharset(contentType));
    }

    public DefaultTextPart(@Nonnull String name, @Nonnull CharSequence value, ContentType contentType, Charset charset) {
        super(name, contentType, charset);
        this.value = value;
    }

    @Override
    protected AbstractByteArrayBody<byte[]> createContentBody() {
        return new TextByteArrayBody();
    }

    @Override
    public CharSequence getContent() {
        return this.value;
    }

    private class TextByteArrayBody extends AbstractByteArrayBody<byte[]> {
        @Override
        protected byte[] contentAsBytes() {
            return BodyUtils.contentAsBytes(value);
        }

        @Override
        public ContentType getContentType() {
            return DefaultTextPart.this.contentType;
        }

        @Override
        public byte[] getContent() {
            return this.getBuffer();
        }
    }
}
