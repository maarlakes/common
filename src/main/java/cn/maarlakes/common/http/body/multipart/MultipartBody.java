package cn.maarlakes.common.http.body.multipart;

import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.RequestBody;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Collection;

/**
 * {@code multipart/form-data} 格式的 HTTP 请求体，包含多个独立的 Part。
 *
 * <p>继承 {@code RequestBody}，内容由 {@link MultipartPart} 集合组成。
 * 每个请求体拥有唯一的 boundary 分隔符，用于在 HTTP 传输中界定各个 Part 的边界。
 * 通过 {@link Builder} 构建实例，支持便捷地添加文本、JSON、二进制和文件类型的 Part。</p>
 *
 * @author linjpxc
 */
public interface MultipartBody extends RequestBody<Collection<? extends MultipartPart<?>>> {

    /**
     * 返回 multipart 消息体的 boundary 分隔符。
     */
    @Nonnull
    String getBoundary();

    /**
     * 创建一个新的 {@link Builder} 实例。
     */
    @Nonnull
    static Builder builder() {
        return new MultipartBodyBuilder();
    }

    /**
     * {@link MultipartBody} 的构建器，提供流式 API 逐个添加 Part 并设置 boundary 和 Content-Type。
     *
     * <p>默认使用 ULID 生成的 boundary 和 {@code multipart/form-data} Content-Type。
     * 通过 {@code addTextPart}、{@code addJsonPart}、{@code addBinaryPart}、{@code addFilePart}
     * 等便捷方法快速添加常见类型的 Part，也可通过 {@link #addPart(MultipartPart)} 添加自定义实现。</p>
     */
    interface Builder {

        /** 设置 boundary 分隔符，替换默认的自动生成值。 */
        @Nonnull
        Builder boundary(@Nonnull String boundary);

        /** 设置整个 multipart 消息体的 Content-Type，默认为 {@code multipart/form-data}。 */
        @Nonnull
        Builder contentType(@Nonnull ContentType contentType);

        @Nonnull
        default Builder addBinaryPart(@Nonnull String name, @Nonnull byte[] buffer) {
            return this.addPart(new ByteArrayPart(name, buffer));
        }

        @Nonnull
        default Builder addBinaryPart(@Nonnull String name, @Nonnull byte[] buffer, Charset charset) {
            return this.addPart(new ByteArrayPart(name, buffer, charset));
        }

        @Nonnull
        default Builder addBinaryPart(@Nonnull String name, @Nonnull byte[] buffer, ContentType contentType) {
            return this.addPart(new ByteArrayPart(name, buffer, contentType));
        }

        @Nonnull
        default Builder addBinaryPart(@Nonnull String name, @Nonnull byte[] buffer, ContentType contentType, Charset charset) {
            return this.addPart(new ByteArrayPart(name, buffer, contentType, charset));
        }

        @Nonnull
        default Builder addTextPart(@Nonnull String name, @Nonnull String value) {
            return this.addPart(new DefaultTextPart(name, value));
        }

        @Nonnull
        default Builder addTextPart(@Nonnull String name, @Nonnull String value, @Nonnull Charset charset) {
            return this.addPart(new DefaultTextPart(name, value, charset));
        }

        @Nonnull
        default Builder addTextPart(@Nonnull String name, @Nonnull String value, ContentType contentType) {
            return this.addPart(new DefaultTextPart(name, value, contentType));
        }

        @Nonnull
        default Builder addTextPart(@Nonnull String name, @Nonnull String value, ContentType contentType, @Nonnull Charset charset) {
            return this.addPart(new DefaultTextPart(name, value, contentType, charset));
        }

        @Nonnull
        default Builder addJsonPart(@Nonnull String name, @Nonnull String json) {
            return this.addPart(new JsonPart(name, json));
        }

        @Nonnull
        default Builder addJsonPart(@Nonnull String name, @Nonnull String json, Charset charset) {
            return this.addPart(new JsonPart(name, json, charset));
        }

        @Nonnull
        default Builder addJsonPart(@Nonnull String name, @Nonnull Object obj) {
            return this.addPart(new JsonPart(name, obj));
        }

        @Nonnull
        default Builder addJsonPart(@Nonnull String name, @Nonnull Object obj, Charset charset) {
            return this.addPart(new JsonPart(name, obj, charset));
        }

        @Nonnull
        default Builder addFilePart(@Nonnull String name, @Nonnull File file) {
            return this.addPart(new DefaultFilePart(name, file));
        }

        @Nonnull
        default Builder addFilePart(@Nonnull String name, @Nonnull File file, Charset charset) {
            return this.addPart(new DefaultFilePart(name, file, charset));
        }

        @Nonnull
        default Builder addFilePart(@Nonnull String name, @Nonnull File file, ContentType contentType) {
            return this.addPart(new DefaultFilePart(name, file, contentType));
        }

        @Nonnull
        default Builder addFilePart(@Nonnull String name, @Nonnull File file, ContentType contentType, Charset charset) {
            return this.addPart(new DefaultFilePart(name, file, contentType, charset));
        }

        /** 添加自定义的 {@link MultipartPart} 实现。 */
        @Nonnull
        Builder addPart(@Nonnull MultipartPart<?> part);

        /** 构建并返回 {@link MultipartBody} 实例。 */
        @Nonnull
        MultipartBody build();
    }
}