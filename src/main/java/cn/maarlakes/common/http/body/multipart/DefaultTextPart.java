package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.body.AbstractByteArrayBody;
import cn.maarlakes.common.http.body.BodyUtils;
import jakarta.annotation.Nonnull;

import java.nio.charset.Charset;

/**
 * {@link TextPart} 的默认实现，将 {@link CharSequence} 内容延迟编码为字节数组。
 *
 * <p>继承 {@link AbstractByteArrayPart} 以复用基于 {@link cn.maarlakes.common.http.body.AbstractByteArrayBody}
 * 的延迟缓存机制。内部通过私有内部类 {@code TextByteArrayBody} 桥接到
 * {@link cn.maarlakes.common.http.body.AbstractByteArrayBody} 的字节缓存体系。
 * 默认 Content-Type 为 {@code text/plain}。</p>
 *
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

    /**
     * 创建用于延迟编码文本内容的内部 Body 实例。
     */
    @Override
    protected AbstractByteArrayBody<byte[]> createContentBody() {
        return new TextByteArrayBody();
    }

    @Override
    public CharSequence getContent() {
        return this.value;
    }

    /**
     * 内部桥接类，将文本内容通过 {@link BodyUtils#contentAsBytes(CharSequence)} 编码为字节数组，
     * 并复用 {@link AbstractByteArrayBody} 的延迟缓存机制。
     */
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
