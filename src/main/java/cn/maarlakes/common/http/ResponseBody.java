package cn.maarlakes.common.http;

import cn.maarlakes.common.function.Consumer3;
import cn.maarlakes.common.utils.StreamUtils;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * HTTP 响应体，提供解码后的内容流。
 *
 * <p>{@link #getContent()} 返回经过编码解码器（gzip/deflate/brotli）处理后的流，
 * {@link #getOriginalContent()} 返回未经解码的原始流。
 *
 * <p>提供便捷方法将响应体转为字符串（{@link #asText()}）、字节数组（{@link #asBytes()}），
 * 或写入输出流（{@link #writeTo}）。这些方法使用后会关闭底层流。
 *
 * @author linjpxc
 */
public interface ResponseBody {

    /**
     * 获取解码后的响应体输入流。
     *
     * <p>如果响应头包含 Content-Encoding（gzip/deflate/brotli），
     * 返回的流会自动经过相应的解码器处理。每次调用返回新的流。
     */
    @Nonnull
    InputStream getContent();

    /**
     * 获取未经解码的原始响应体输入流。每次调用返回新的流。
     */
    @Nonnull
    InputStream getOriginalContent();

    /** 响应体的 Content-Type，可为 null。 */
    ContentType getContentType();

    /** Content-Encoding 头部值，可为 null。 */
    Header getContentEncoding();

    @Nonnull
    default Charset getCharset() {
        final ContentType contentType = this.getContentType();
        if (contentType != null && contentType.getCharset() != null) {
            return Charset.forName(contentType.getCharset());
        }
        return StandardCharsets.UTF_8;
    }

    default String asText() {
        return this.asText(this.getCharset());
    }

    default String asText(@Nonnull Charset charset) {
        try (InputStream content = this.getContent()) {
            return StreamUtils.readAllText(content, charset);
        } catch (Exception e) {
            throw new HttpClientException(e.getMessage(), e);
        }
    }

    default byte[] asBytes() {
        try (InputStream content = this.getContent()) {
            return StreamUtils.readAllBytes(content);
        } catch (Exception e) {
            throw new HttpClientException(e.getMessage(), e);
        }
    }

    default void writeTo(@Nonnull OutputStream out) {
        this.writeTo(out::write);
    }

    default void writeTo(@Nonnull Consumer3<byte[], Integer, Integer> consumer) {
        try (InputStream content = this.getContent()) {
            StreamUtils.writeTo(content, consumer);
        } catch (Exception e) {
            throw new HttpClientException(e.getMessage(), e);
        }
    }
}
