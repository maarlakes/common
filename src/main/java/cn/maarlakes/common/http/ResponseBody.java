package cn.maarlakes.common.http;

import cn.maarlakes.common.function.Consumer3;
import cn.maarlakes.common.utils.StreamUtils;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Nonnull;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author linjpxc
 */
public interface ResponseBody {

    @Nonnull
    InputStream getContent();

    ContentType getContentType();

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
            throw new IllegalStateException(e);
        }
    }

    default byte[] asBytes() {
        try (InputStream content = this.getContent()) {
            return StreamUtils.readAllBytes(content);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    default <T> T toJsonObject(@Nonnull Class<T> type) {
        return this.toJsonObject(type, this.getCharset());
    }

    default <T> T toJsonObject(@Nonnull Class<T> type, @Nonnull Charset charset) {
        try (InputStream content = this.getContent()) {
            return JSON.parseObject(content, charset, type);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    default void writeTo(@Nonnull OutputStream out) {
        this.writeTo(out::write);
    }

    default void writeTo(@Nonnull Consumer3<byte[], Integer, Integer> consumer) {
        try (InputStream content = this.getContent()) {
            StreamUtils.writeTo(content, consumer);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
