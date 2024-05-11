package cn.maarlakes.common.http.body.multipart;

/**
 * @author linjpxc
 */
public interface TextPart extends MultipartPart<CharSequence> {
}
//public class TextPart extends AbstractByteArrayPart<CharSequence> {
//
//    private final CharSequence value;
//
//    public TextPart(@Nonnull String name, @Nonnull CharSequence value) {
//        this(name, value, ContentType.TEXT_PLAIN, null);
//    }
//
//    public TextPart(@Nonnull String name, @Nonnull CharSequence value, Charset charset) {
//        this(name, value, ContentType.TEXT_PLAIN, charset);
//    }
//
//    public TextPart(@Nonnull String name, @Nonnull CharSequence value, ContentType contentType) {
//        this(name, value, contentType, toCharset(contentType));
//    }
//
//    public TextPart(@Nonnull String name, @Nonnull CharSequence value, ContentType contentType, Charset charset) {
//        super(name, contentType, charset);
//        this.value = value;
//    }
//
//    @Override
//    protected AbstractByteArrayBody<byte[]> createContentBody() {
//        return new TextByteArrayBody();
//    }
//
//    @Override
//    public CharSequence getContent() {
//        return this.value;
//    }
//
//    private class TextByteArrayBody extends AbstractByteArrayBody<byte[]> {
//        @Override
//        protected byte[] contentAsBytes() {
//            return BodyUtils.contentAsBytes(value);
//        }
//
//        @Override
//        public ContentType getContentType() {
//            return TextPart.this.contentType;
//        }
//
//        @Override
//        public byte[] getContent() {
//            return this.getBuffer();
//        }
//    }
//}
