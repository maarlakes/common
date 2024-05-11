package cn.maarlakes.common.http.body;

import cn.maarlakes.common.http.ContentType;
import jakarta.annotation.Nonnull;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author linjpxc
 */
public class DefaultTextBody extends AbstractByteArrayBody<CharSequence> implements TextBody {
    private final CharSequence content;
    private final ContentType contentType;

    public DefaultTextBody(@Nonnull CharSequence content) {
        this(content, ContentType.TEXT_PLAIN, StandardCharsets.UTF_8);
    }

    public DefaultTextBody(@Nonnull CharSequence content, Charset charset) {
        this(content, ContentType.TEXT_PLAIN, charset);
    }

    public DefaultTextBody(@Nonnull CharSequence content, @Nonnull ContentType contentType) {
        this(content, contentType, (String) null);
    }

    public DefaultTextBody(@Nonnull CharSequence content, @Nonnull ContentType contentType, Charset charset) {
        this.content = content;
        this.contentType = charset == null ? contentType : contentType.withCharset(charset);
    }

    public DefaultTextBody(@Nonnull CharSequence content, @Nonnull ContentType contentType, String charset) {
        this.content = content;
        this.contentType = charset == null ? contentType : contentType.withCharset(charset);
    }

    @Override
    public ContentType getContentType() {
        return this.contentType;
    }

    @Override
    public CharSequence getContent() {
        return this.content;
    }

    @Override
    protected byte[] contentAsBytes() {
        return BodyUtils.contentAsBytes(this.content, this.contentType.getCharset());
    }
}
