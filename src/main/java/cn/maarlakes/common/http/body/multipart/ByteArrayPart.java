package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.function.Consumer3;
import cn.maarlakes.common.http.ContentType;
import jakarta.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 二进制字节数组类型的 multipart Part，直接持有 {@code byte[]} 内容。
 *
 * <p>适用于上传二进制数据的场景（如图片、压缩包等）。
 * 由于内容已经是字节数组，无需经过编码转换，写入时直接传输原始字节。
 * 默认 Content-Type 为空（由使用者按需指定），字符集 UTF-8。</p>
 *
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
