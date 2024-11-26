package cn.maarlakes.common.http;

import cn.maarlakes.common.http.encoder.ResponseBodyEncoder;
import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.Lazy;
import cn.maarlakes.common.utils.StreamUtils;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Nonnull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.StreamSupport;

/**
 * @author linjpxc
 */
public class ByteArrayResponseBody implements ResponseBody {

    private static final Lazy<ResponseBodyEncoder[]> DEFAULT_ENCODERS = Lazy.of(() -> SpiServiceLoader.loadShared(ResponseBodyEncoder.class, ResponseBodyEncoder.class.getClassLoader()).stream().toArray(ResponseBodyEncoder[]::new));

    private final byte[] content;
    private final ContentType contentType;
    private final Header contentEncoding;
    private final Lazy<ResponseBodyEncoder[]> encoders;

    public ByteArrayResponseBody(@Nonnull byte[] content, ContentType contentType, Header contentEncoding) {
        this(content, contentType, contentEncoding, DEFAULT_ENCODERS);
    }

    public ByteArrayResponseBody(@Nonnull byte[] content, ContentType contentType, Header contentEncoding, ResponseBodyEncoder... encoders) {
        this(content, contentType, contentEncoding, Lazy.of(() -> encoders));
    }

    public ByteArrayResponseBody(@Nonnull byte[] content, ContentType contentType, Header contentEncoding, Iterable<ResponseBodyEncoder> encoders) {
        this(content, contentType, contentEncoding, Lazy.of(() -> StreamSupport.stream(encoders.spliterator(), false).toArray(ResponseBodyEncoder[]::new)));
    }

    private ByteArrayResponseBody(@Nonnull byte[] content, ContentType contentType, Header contentEncoding, Lazy<ResponseBodyEncoder[]> encoders) {
        this.content = content;
        this.contentType = contentType;
        this.contentEncoding = contentEncoding;

        this.encoders = encoders;
    }

    @Nonnull
    @Override
    public InputStream getContent() {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(this.content);
        if (this.contentEncoding != null) {
            for (ResponseBodyEncoder encoder : this.encoders.get()) {
                if (encoder.supported(this.contentEncoding)) {
                    return encoder.decoding(inputStream);
                }
            }
        }
        return inputStream;
    }

    @Nonnull
    @Override
    public InputStream getOriginalContent() {
        return new ByteArrayInputStream(this.content);
    }

    @Override
    public ContentType getContentType() {
        return this.contentType;
    }

    @Override
    public Header getContentEncoding() {
        return this.contentEncoding;
    }

    @Override
    public String asText(@Nonnull Charset charset) {
        return new String(this.contentUnzip(), charset);
    }

    @Override
    public byte[] asBytes() {
        return Arrays.copyOf(this.contentUnzip(), this.content.length);
    }

    @Override
    public <T> T toJsonObject(@Nonnull Class<T> type, @Nonnull Charset charset) {
        return JSON.parseObject(this.contentUnzip(), 0, this.content.length, charset, type);
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

    private byte[] contentUnzip() {
        if (this.contentEncoding != null) {
            try (InputStream inputStream = this.getContent()) {
                for (ResponseBodyEncoder encoder : this.encoders.get()) {
                    if (encoder.supported(this.contentEncoding)) {
                        return StreamUtils.readAllBytes(encoder.decoding(inputStream));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return this.content;
    }
}
